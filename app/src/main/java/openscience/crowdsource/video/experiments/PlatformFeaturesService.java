package openscience.crowdsource.video.experiments;

import android.os.Build;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import static openscience.crowdsource.video.experiments.AppConfigService.cachedPlatformFeaturesFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.path_opencl;
import static openscience.crowdsource.video.experiments.AppConfigService.repo_uoa;
import static openscience.crowdsource.video.experiments.MainActivity.pf_gpu;
import static openscience.crowdsource.video.experiments.MainActivity.pf_gpu_vendor;
import static openscience.crowdsource.video.experiments.Utils.exchange_info_with_ck_server;
import static openscience.crowdsource.video.experiments.Utils.get_cpu_freqs;

/**
 * @author Daniil Efremov
 */

public class PlatformFeaturesService {


    synchronized public static void cleanuCachedPlatformFeaturesF() {
        File file = new File(cachedPlatformFeaturesFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public static JSONObject loadPlatformFeatures() {
        File file = new File(cachedPlatformFeaturesFilePath);
        if (file.exists()) {
            try {
                JSONObject dict = openme.openme_load_json_file(cachedPlatformFeaturesFilePath);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                return dict.getJSONObject("dict");
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read preloaded file " + cachedPlatformFeaturesFilePath);
                return null;
            }
        } else {
            return loadPlatformFeaturesFromServer();
        }
    }

    public static void savePlatformFeaturesToFile(JSONObject platformFetaturesJSONObject) {
        File scenariosFile = new File(cachedPlatformFeaturesFilePath);
        if (scenariosFile.exists()) {
            try {
                openme.openme_store_json_file(platformFetaturesJSONObject, cachedPlatformFeaturesFilePath);
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could save platform features to file " + cachedPlatformFeaturesFilePath + " because of the error " + e.getLocalizedMessage());
                return;
            }
        }
    }


    public static JSONObject loadPlatformFeaturesFromServer() {
        String pf_system = "";
        String pf_system_vendor = "";
        String pf_system_model = "";
        String pf_cpu = "";
        String pf_cpu_subname = "";
        String pf_cpu_features = "";
        String pf_cpu_abi = "";
        String pf_cpu_num = "";
        String pf_gpu_opencl = "";
        String pf_gpu_openclx = "";
        String pf_memory = "";
        String pf_os = "";
        String pf_os_short = "";
        String pf_os_long = "";
        String pf_os_bits = "32";

        JSONObject r = null;
        JSONObject requestObject = null;
        JSONObject ft_cpu = null;
        JSONObject ft_os = null;
        JSONObject ft_gpu = null;
        JSONObject ft_plat = null;
        JSONObject ftuoa = null;

        AppLogger.logMessage("\n"); //s_line);
        AppLogger.logMessage("User ID: " + AppConfigService.getEmail() + "\n");

        AppLogger.logMessage("\n");
        AppLogger.logMessage("Testing Collective Knowledge server ...\n");
        if (AppConfigService.getRemoteServerURL() == null) {
            AppLogger.logMessage("\n Error Collective Knowledge server is not reachible ...\n\n");
            return null;
        }
        requestObject = new JSONObject();
        try {
            requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());
            requestObject.put("action", "test");
            requestObject.put("module_uoa", "program.optimization");
            requestObject.put("email", AppConfigService.getEmail());
            requestObject.put("type", "mobile-crowdtuning");
            requestObject.put("out", "json");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError with JSONObject ...\n\n");
            return null;
        }

        try {
            r = openme.remote_access(requestObject);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (Utils.validateReturnCode(r)) return null;

        String status = "";
        try {
            status = (String) r.get("status");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError obtaining key 'string' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        AppLogger.logMessage("    " + status + "\n");

        PFInfo pfInfo = null;



        DeviceInfo deviceInfo = new DeviceInfo();

        /*********** Getting local information about platform **************/
        AppLogger.logMessage("Detecting some of your platform features ...\n");

        //Get system info **************************************************
        try {
            r = openme.read_text_file_and_convert_to_json("/system/build.prop", "=", false, false);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                AppLogger.logMessage("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        String model = "";
        String manu = "";

        JSONObject dict = null;

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (dict != null) {
            try {
                model = (String) dict.get("ro.product.model");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                manu = (String) dict.get("ro.product.manufacturer");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (!model.equals("") && !manu.equals(""))
                if (model.toLowerCase().startsWith(manu.toLowerCase()))
                    model = model.substring(manu.length() + 1, model.length());

            if (manu.equals("") && !model.equals("")) manu = model;

            manu = manu.toUpperCase();
            model = model.toUpperCase();

            pf_system = manu;
            if (!model.equals("")) pf_system += ' ' + model;
            pf_system_model = model;
            pf_system_vendor = manu;
        }

        //Get processor info **************************************************
        //It's not yet working properly on heterogeneous CPU, like big/little
        //So results can't be trusted and this part should be improved!
        try {
            r = openme.read_text_file_and_convert_to_json("/proc/cpuinfo", ":", false, false);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                AppLogger.logMessage("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        String processor_file = null;

        try {
            processor_file = (String) r.get("file_as_string");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }
        if (processor_file == null) processor_file = "";

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (dict != null) {
            String processor = null;
            String modelName = null;
            String hardWare = null;
            String features = null;

            try {
                processor = (String) dict.get("Processor");
            } catch (JSONException e) {
            }
            try {
                modelName = (String) dict.get("model_name");
            } catch (JSONException e) {
            }
            try {
                hardWare = (String) dict.get("Hardware");
            } catch (JSONException e) {
            }
            try {
                features = (String) dict.get("Features");
            } catch (JSONException e) {
            }

            if (modelName != null && !modelName.equals("")) pf_cpu_subname = modelName;
            else if (processor != null && !processor.equals("")) pf_cpu_subname = processor;

            if (hardWare != null) pf_cpu = hardWare;
            if (features != null) pf_cpu_features = features;

            // On Intel-based Android
            if (pf_cpu.equals("") && !pf_cpu_subname.equals(""))
                pf_cpu = pf_cpu_subname;

            // If CPU is empty, possibly new format
            if (pf_cpu.equals("")) {
                String cpuImplementer = "";
                String cpuArchitecture = "";
                String cpuVariant = "";
                String cpuPart = "";
                String cpuRevision = "";

                try {
                    cpuImplementer = (String) dict.get("CPU implementer");
                } catch (JSONException e) {
                }

                try {
                    cpuArchitecture = (String) dict.get("CPU architecture");
                } catch (JSONException e) {
                }

                try {
                    cpuVariant = (String) dict.get("CPU variant");
                } catch (JSONException e) {
                }

                try {
                    cpuPart = (String) dict.get("CPU part");
                } catch (JSONException e) {
                }

                try {
                    cpuRevision = (String) dict.get("CPU revision");
                } catch (JSONException e) {
                }

                pf_cpu += cpuImplementer + "-" + cpuArchitecture + "-" + cpuVariant + "-" + cpuPart + "-" + cpuRevision;
            }

            // If CPU is still empty, send report to CK to fix ...
            if (pf_cpu.equals("")) {
                AppLogger.logMessage("\nPROBLEM: we could not detect CPU name and features on your device :( ! Please, report to authors!\n\n");
                if (AppConfigService.getRemoteServerURL() == null) {
                    AppLogger.logMessage("\n Error we could not report about CPU name and feature detection problem to Collective Knowledge server: it's not reachible ...\n\n");
                    return null;
                }
                requestObject = new JSONObject();
                try {
                    requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());//
                    requestObject.put("action", "problem");
                    requestObject.put("module_uoa", "program.optimization");
                    requestObject.put("email", AppConfigService.getEmail());
                    requestObject.put("problem", "mobile_crowdtuning_cpu_name_empty");
                    requestObject.put("problem_data", processor_file);
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    AppLogger.logMessage("\nError with JSONObject ...\n\n");
                    return null;
                }

                try {
                    r = openme.remote_access(requestObject);
                } catch (JSONException e) {
                    AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (Utils.validateReturnCode(r)) return null;

                return null;
            }
        }

        //Get memory info **************************************************
        try {
            r = openme.read_text_file_and_convert_to_json("/proc/meminfo", ":", false, false);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0 && (Long) r.get("return") != 16)
                AppLogger.logMessage("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (dict != null) {
            String mem_tot = "";

            try {
                mem_tot = (String) dict.get("memtotal");
            } catch (JSONException e) {
            }

            if (mem_tot == null || mem_tot.equals(""))
                try {
                    mem_tot = (String) dict.get("MemTotal");
                } catch (JSONException e) {
                }

            if (mem_tot != null && !mem_tot.equals("")) {
                int i = mem_tot.indexOf(' ');
                if (i > 0) {
                    String mem1 = mem_tot.substring(0, i).trim();
                    String mem2 = "1";
                    if (mem1.length() > 3) mem2 = mem1.substring(0, mem1.length() - 3);
                    pf_memory = mem2 + " MB";
                }
            }
        }

        //Get available processors and frequencies **************************************************
        List<Double[]> cpus = get_cpu_freqs();
        Double[] cpu = null;

        int cpu_num = cpus.size();
        pf_cpu_num = Integer.toString(cpu_num);

        pf_cpu_abi = Build.CPU_ABI; //System.getProperty("os.arch"); - not exactly the same!

        //Get OS info **************************************************
        pf_os = "Android " + Build.VERSION.RELEASE;

        try {
            r = openme.read_text_file_and_convert_to_json("/proc/version", ":", false, false);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                AppLogger.logMessage("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            pf_os_long = (String) r.get("file_as_string");
        } catch (JSONException e) {
        }
        if (pf_os_long == null) pf_os_long = "";
        else {
            pf_os_long = pf_os_long.trim();

            pf_os_short = pf_os_long;

            int ix = pf_os_long.indexOf(" (");

            if (ix >= 0) {
                int ix1 = pf_os_long.indexOf("-");
                if (ix1 >= 0 && ix1 < ix) ix = ix1;
                pf_os_short = pf_os_long.substring(0, ix).trim();
            }
        }

        int ix = pf_os_long.indexOf("_64");
        if (ix > 0) pf_os_bits = "64";

        //Get OpenCL info **************************************************
        File fopencl = new File(path_opencl);
        long lfopencl = fopencl.length();

        if (lfopencl > 0) {
            pf_gpu_opencl = "libOpenCL.so - found (" + Long.toString(lfopencl) + " bytes)";
            pf_gpu_openclx = "yes";
        } else {
            pf_gpu_opencl = "libOpenCL.so - not found";
            pf_gpu_openclx = "no";
        }

        //Print **************************************************
        AppLogger.logMessage("\n");
        AppLogger.logMessage("PLATFORM:   " + pf_system + "\n");
        AppLogger.logMessage("* VENDOR:   " + pf_system_vendor + "\n");
        AppLogger.logMessage("* MODEL:   " + pf_system_model + "\n");
        AppLogger.logMessage("OS:   " + pf_os + "\n");
        AppLogger.logMessage("* SHORT:   " + pf_os_short + "\n");
        AppLogger.logMessage("* LONG:   " + pf_os_long + "\n");
        AppLogger.logMessage("* BITS:   " + pf_os_bits + "\n");
        AppLogger.logMessage("MEMORY:   " + pf_memory + "\n");
        AppLogger.logMessage("CPU:   " + pf_cpu + "\n");
        AppLogger.logMessage("* SUBNAME:   " + pf_cpu_subname + "\n");
        AppLogger.logMessage("* ABI:   " + pf_cpu_abi + "\n");
        AppLogger.logMessage("* FEATURES:   " + pf_cpu_features + "\n");
        AppLogger.logMessage("* CORES:   " + pf_cpu_num + "\n");
        for (int i = 0; i < cpu_num; i++) {
            String x = "    " + Integer.toString(i) + ") ";
            cpu = cpus.get(i);
            double x0 = cpu[0];
            ;
            double x1 = cpu[1];
            double x2 = cpu[2];

            if (x0 == 0) x += "offline";
            else
                x += "online; " + Double.toString(x2) + " of " + Double.toString(x1) + " MHz";

            AppLogger.logMessage(x + "\n");
        }
        AppLogger.logMessage("GPU:   " + pf_gpu + "\n");
        AppLogger.logMessage("* VENDOR:   " + pf_gpu_vendor + "\n");
        AppLogger.logMessage("* OPENCL:   " + pf_gpu_opencl + "\n");

        //Delay program for 1 sec
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        //Communicate with CK **************************************************
        JSONObject j_os, j_cpu, j_gpu, j_sys;

        String j_os_uid = "";
        String j_cpu_uid = "";
        String j_gpu_uid = "";
        String j_sys_uid = "";

//            AppLogger.logMessage(s_line);
        AppLogger.logMessage("Exchanging info about your platform with CK server to retrieve latest meta for crowdtuning ...");

        requestObject = new JSONObject();
        JSONObject platformFeatures = new JSONObject();

        // OS ******
        AppLogger.logMessage("\n    Exchanging OS info ...\n");

        try {
            ft_os = new JSONObject();

            ft_os.put("name", pf_os);
            ft_os.put("name_long", pf_os_long);
            ft_os.put("name_short", pf_os_short);
            ft_os.put("bits", pf_os_bits);

            platformFeatures.put("features", ft_os);

            if (AppConfigService.getRemoteServerURL() == null) {
                AppLogger.logMessage("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform.os");
            requestObject.put("data_name", pf_os);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError with JSONObject ...\n\n");
            return null;
        }

        j_os = exchange_info_with_ck_server(requestObject);
        int rr = 0;
        try {
            Object rx = j_os.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            AppLogger.logMessage("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_os.get("error");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            AppLogger.logMessage("\nProblem accessing CK server: " + err + "\n");
            return null;
        } else {
            String found = "";
            try {
                found = (String) j_os.get("found");
            } catch (JSONException e) {
            }

            if (found.equals("yes")) {
                try {
                    j_os_uid = (String) j_os.get("data_uid");
                } catch (JSONException e) {
                }

                String x = "         Already exists";
                if (!j_os_uid.equals("")) x += " (CK UOA=" + j_os_uid + ")";
                x += "!\n";
                AppLogger.logMessage(x);
            }
        }

        // GPU ******
        if (!pf_gpu.equals("") && AppConfigService.getRemoteServerURL() != null) {
            AppLogger.logMessage("    Exchanging GPU info ...\n");

            try {
                ft_gpu = new JSONObject();

                ft_gpu.put("name", pf_gpu);
                ft_gpu.put("vendor", pf_gpu_vendor);

                platformFeatures.put("features", ft_gpu);

                requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());//
                requestObject.put("action", "exchange");
                requestObject.put("module_uoa", "platform");
                requestObject.put("sub_module_uoa", "platform.gpu");
                requestObject.put("data_name", pf_gpu);
                requestObject.put("repo_uoa", repo_uoa);
                requestObject.put("all", "yes");
                requestObject.put("dict", platformFeatures);
                requestObject.put("out", "json");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError with JSONObject ...\n\n");
                return null;
            }

            j_gpu = exchange_info_with_ck_server(requestObject);
            try {
                Object rx = j_gpu.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                AppLogger.logMessage("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) j_gpu.get("error");
                } catch (JSONException e) {
                    AppLogger.logMessage("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                AppLogger.logMessage("\nProblem accessing CK server: " + err + "\n");
                return null;
            } else {
                String found = "";
                try {
                    found = (String) j_gpu.get("found");
                } catch (JSONException e) {
                }

                if (found.equals("yes")) {
                    try {
                        j_gpu_uid = (String) j_gpu.get("data_uid");
                    } catch (JSONException e) {
                    }

                    String x = "         Already exists";
                    if (!j_gpu_uid.equals("")) x += " (CK UOA=" + j_gpu_uid + ")";
                    x += "!\n";
                    AppLogger.logMessage(x);
                }
            }
        }

        // CPU ******
        AppLogger.logMessage("    Exchanging CPU info ...\n");

        try {
            ft_cpu = new JSONObject();

            ft_cpu.put("name", pf_cpu);
            ft_cpu.put("sub_name", pf_cpu_subname);
            ft_cpu.put("cpu_features", pf_cpu_features);
            ft_cpu.put("cpu_abi", pf_cpu_abi);
            ft_cpu.put("num_proc", pf_cpu_num);

            JSONObject freq_max = new JSONObject();
            for (int i = 0; i < cpu_num; i++) {
                String x = "    " + Integer.toString(i) + ") ";
                cpu = cpus.get(i);
                double x1 = cpu[1];
                if (x1 != 0)
                    freq_max.put(Integer.toString(i), x1);
            }
            ft_cpu.put("max_freq", freq_max);

            platformFeatures.put("features", ft_cpu);

            if (AppConfigService.getRemoteServerURL() == null) {
                AppLogger.logMessage("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform.cpu");
            requestObject.put("data_name", pf_cpu);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError with JSONObject ...\n\n");
            return null;
        }

        j_cpu = exchange_info_with_ck_server(requestObject);
        try {
            Object rx = j_cpu.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            AppLogger.logMessage("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_cpu.get("error");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            AppLogger.logMessage("\nProblem accessing CK server: " + err + "\n");
            return null;
        } else {
            String found = "";
            try {
                found = (String) j_cpu.get("found");
            } catch (JSONException e) {
            }

            if (found.equals("yes")) {
                try {
                    j_cpu_uid = (String) j_cpu.get("data_uid");
                } catch (JSONException e) {
                }

                String x = "         Already exists";
                if (!j_cpu_uid.equals("")) x += " (CK UOA=" + j_cpu_uid + ")";
                x += "!\n";
                AppLogger.logMessage(x);
            }
        }

        // Platform ******
        AppLogger.logMessage("    Exchanging platform info ...\n");

        try {
            ft_plat = new JSONObject();

            ft_plat.put("name", pf_system);
            ft_plat.put("vendor", pf_system_vendor);
            ft_plat.put("model", pf_system_model);

            platformFeatures.put("features", ft_plat);

            if (AppConfigService.getRemoteServerURL() == null) {
                AppLogger.logMessage("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", AppConfigService.getRemoteServerURL());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform");
            requestObject.put("data_name", pf_system);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError with JSONObject ...\n\n");
            return null;
        }

        j_sys = exchange_info_with_ck_server(requestObject);
        try {
            Object rx = j_sys.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            AppLogger.logMessage("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_sys.get("error");
            } catch (JSONException e) {
                AppLogger.logMessage("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            AppLogger.logMessage("\nProblem accessing CK server: " + err + "\n");
            return null;
        } else {
            String found = "";
            try {
                found = (String) j_sys.get("found");
            } catch (JSONException e) {
            }

            if (found.equals("yes")) {
                try {
                    j_sys_uid = (String) j_sys.get("data_uid");
                } catch (JSONException e) {
                }

                String x = "         Already exists";
                if (!j_sys_uid.equals("")) x += " (CK UOA=" + j_sys_uid + ")";
                x += "!\n";
                AppLogger.logMessage(x);
            }
        }

        //Delay program for 1 sec
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        deviceInfo.setJ_os_uid(j_os_uid);
        deviceInfo.setJ_cpu_uid(j_cpu_uid);
        deviceInfo.setJ_gpu_uid(j_gpu_uid);
        deviceInfo.setJ_sys_uid(j_sys_uid);


        pfInfo = new PFInfo();
        pfInfo.setPf_system(pf_system);
        pfInfo.setPf_system_vendor(pf_system_vendor);
        pfInfo.setPf_system_model(pf_system_model);
        pfInfo.setPf_cpu(pf_cpu);
        pfInfo.setPf_cpu_subname(pf_cpu_subname);
        pfInfo.setPf_cpu_features(pf_cpu_features);
        pfInfo.setPf_cpu_abi(pf_cpu_abi);
        pfInfo.setPf_cpu_num(pf_cpu_num);
        pfInfo.setPf_gpu_opencl(pf_gpu_opencl);
        pfInfo.setPf_gpu_openclx(pf_gpu_openclx);
        pfInfo.setPf_memory(pf_memory);
        pfInfo.setPf_os(pf_os);
        pfInfo.setPf_os_short(pf_os_short);
        pfInfo.setPf_os_long(pf_os_long);
        pfInfo.setPf_os_bits(pf_os_bits);

        try {
            platformFeatures = getPlatformFeaturesJSONObject(pf_gpu_openclx, ft_cpu, ft_os, ft_gpu, ft_plat, deviceInfo);
            openme.openme_store_json_file(platformFeatures, cachedPlatformFeaturesFilePath);
        } catch (JSONException e) {
            AppLogger.logMessage("Error with platformFeatures ...");
            return null;
        }
        AppLogger.logMessage("Platform Features successfully loaded");
        return loadPlatformFeatures();
    }

    public static JSONObject getPlatformFeaturesJSONObject(String pf_gpu_openclx, JSONObject ft_cpu, JSONObject ft_os, JSONObject ft_gpu, JSONObject ft_plat, DeviceInfo deviceInfo) throws JSONException {
        JSONObject ft;
        ft = new JSONObject();

        ft.put("cpu", ft_cpu);
        ft.put("cpu_uid", deviceInfo.getJ_cpu_uid());
        ft.put("cpu_uoa", deviceInfo.getJ_cpu_uid());

        ft.put("gpu", ft_gpu);
        ft.put("gpu_uid", deviceInfo.getJ_gpu_uid());
        ft.put("gpu_uoa", deviceInfo.getJ_gpu_uid());

        // Need to tell CK server if OpenCL present
        // for collaborative OpenCL optimization using mobile devices
        JSONObject ft_gpu_misc = new JSONObject();
        ft_gpu_misc.put("opencl_lib_present", pf_gpu_openclx);
        ft.put("gpu_misc", ft_gpu_misc);

        ft.put("os", ft_os);
        ft.put("os_uid", deviceInfo.getJ_os_uid());
        ft.put("os_uoa", deviceInfo.getJ_os_uid());

        ft.put("platform", ft_plat);
        ft.put("platform_uid", deviceInfo.getJ_sys_uid());
        ft.put("platform_uoa", deviceInfo.getJ_sys_uid());
        return ft;
    }
}
