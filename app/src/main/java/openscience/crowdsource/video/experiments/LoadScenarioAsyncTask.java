package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static java.lang.Thread.sleep;
import static openscience.crowdsource.video.experiments.AppConfigService.cachedPlatformFeaturesFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.cachedScenariosFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpensciencePath;
import static openscience.crowdsource.video.experiments.AppConfigService.path_opencl;
import static openscience.crowdsource.video.experiments.AppConfigService.repo_uoa;
import static openscience.crowdsource.video.experiments.AppConfigService.url_cserver;
import static openscience.crowdsource.video.experiments.MainActivity.pf_gpu;
import static openscience.crowdsource.video.experiments.MainActivity.pf_gpu_vendor;
import static openscience.crowdsource.video.experiments.RecognitionScenarioService.updateProgress;
import static openscience.crowdsource.video.experiments.Utils.exchange_info_with_ck_server;
import static openscience.crowdsource.video.experiments.Utils.get_cpu_freqs;

/**
 * @author Daniil Efremov
 */
public class LoadScenarioAsyncTask extends AsyncTask<RecognitionScenario, String, String> {

    /*************************************************************************/
    public String get_shared_computing_resource(String url) {
        String s = "";

        try {
            //Connect
            URL u = new URL(url);

            URLConnection urlc = u.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

            String line = "";
            while ((line = br.readLine()) != null)
                s += line + '\n';
            br.close();
        } catch (Exception e) {
            publishProgress("Error shared computing resource is not reachable " + e.getLocalizedMessage() + "...\n\n");
            return null;
        }

            /* Trying to convert to dict from JSON */
        JSONObject a = null;

        try {
            a = new JSONObject(s);
        } catch (JSONException e) {
            publishProgress("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
            return null;
        }

            /* For now just take default one, later add random or balancing */
        JSONObject rs = null;
        try {
            if (a.has("default"))
                rs = (JSONObject) a.get("default");
            if (rs != null) {
                if (rs.has("url"))
                    s = (String) rs.get("url");
            }
        } catch (JSONException e) {
            publishProgress("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
            return null;
        }

        if (s == null)
            s = "";
        else if (!s.endsWith("?"))
            s += "/?";

        publishProgress("\n");

        if (s.startsWith("ERROR")) {
            publishProgress(s);
            publishProgress("\n");
            return null;
        } else {
            publishProgress("Public Collective Knowledge Server found:\n");
            publishProgress(s);
            publishProgress("\n");
        }

        return s;
    }

    /*************************************************************************/
    public void copy_bin_file(String src, String dst) throws IOException {
        File fin = new File(src);
        File fout = new File(dst);

        InputStream in = new FileInputStream(fin);
        OutputStream out = new FileOutputStream(fout);

        // Transfer bytes from in to out
        int l = 0;
        byte[] buf = new byte[16384];
        while ((l = in.read(buf)) > 0) out.write(buf, 0, l);

        in.close();
        out.close();
    }

    /*************************************************************************/
    protected void onPostExecute(String x) {
        AppConfigService.AppConfig.State state = AppConfigService.getState();
        if (state == null || state.equals(AppConfigService.AppConfig.State.IN_PROGRESS)) {
            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        }
    }

    /*************************************************************************/
    protected void onProgressUpdate(String... values) {
        if (values[0] != "") {
            AppLogger.logMessage(values[0]);
        } else if (values[1] != "") {
            AppLogger.logMessage("Error onProgressUpdate " + values[1]);
        }
    }

    /*************************************************************************/
    @Override
    protected String doInBackground(RecognitionScenario... arg0) {

        RecognitionScenario recognitionScenario = arg0[0];
        // todo remove debug infor
        if (recognitionScenario.getState() != RecognitionScenario.State.DOWNLOADED) {
            while(recognitionScenario.getDownloadedTotalFileSizeBytes() < recognitionScenario.getTotalFileSizeBytes()) {
                recognitionScenario.setDownloadedTotalFileSizeBytes(recognitionScenario.getDownloadedTotalFileSizeBytes() + 1000000);
                recognitionScenario.setState(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS);
                updateProgress(recognitionScenario);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    AppLogger.logMessage("Error " + e.getLocalizedMessage());
                }
            }
            recognitionScenario.setState(RecognitionScenario.State.DOWNLOADED);
            updateProgress(recognitionScenario);
        }
        if (1==1) return null;

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

        publishProgress("\n"); //s_line);
        publishProgress("User ID: " + AppConfigService.getEmail() + "\n");

        publishProgress("\n");
        publishProgress("Testing Collective Knowledge server ...\n");
        if (getCurl() == null) {
            publishProgress("\n Error Collective Knowledge server is not reachible ...\n\n");
            return null;
        }
        requestObject = new JSONObject();
        try {
            requestObject.put("remote_server_url", getCurl());
            requestObject.put("action", "test");
            requestObject.put("module_uoa", "program.optimization");
            requestObject.put("email", AppConfigService.getEmail());
            requestObject.put("type", "mobile-crowdtuning");
            requestObject.put("out", "json");
        } catch (JSONException e) {
            publishProgress("\nError with JSONObject ...\n\n");
            return null;
        }

        try {
            r = openme.remote_access(requestObject);
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (validateReturnCode(r)) return null;

        String status = "";
        try {
            status = (String) r.get("status");
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'string' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        publishProgress("    " + status + "\n");

        PFInfo pfInfo = null;



        DeviceInfo deviceInfo = new DeviceInfo();

        /*********** Getting local information about platform **************/
        publishProgress("Detecting some of your platform features ...\n");

        //Get system info **************************************************
        try {
            r = openme.read_text_file_and_convert_to_json("/system/build.prop", "=", false, false);
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        String model = "";
        String manu = "";

        JSONObject dict = null;

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (dict != null) {
            try {
                model = (String) dict.get("ro.product.model");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                manu = (String) dict.get("ro.product.manufacturer");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
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
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        String processor_file = null;

        try {
            processor_file = (String) r.get("file_as_string");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }
        if (processor_file == null) processor_file = "";

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
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
                publishProgress("\nPROBLEM: we could not detect CPU name and features on your device :( ! Please, report to authors!\n\n");
                if (getCurl() == null) {
                    publishProgress("\n Error we could not report about CPU name and feature detection problem to Collective Knowledge server: it's not reachible ...\n\n");
                    return null;
                }
                requestObject = new JSONObject();
                try {
                    requestObject.put("remote_server_url", getCurl());//
                    requestObject.put("action", "problem");
                    requestObject.put("module_uoa", "program.optimization");
                    requestObject.put("email", AppConfigService.getEmail());
                    requestObject.put("problem", "mobile_crowdtuning_cpu_name_empty");
                    requestObject.put("problem_data", processor_file);
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                try {
                    r = openme.remote_access(requestObject);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (validateReturnCode(r)) return null;

                return null;
            }
        }

        //Get memory info **************************************************
        try {
            r = openme.read_text_file_and_convert_to_json("/proc/meminfo", ":", false, false);
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0 && (Long) r.get("return") != 16)
                publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            dict = (JSONObject) r.get("dict");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
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
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        try {
            if ((Long) r.get("return") > 0)
                publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
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
        publishProgress("\n");
        publishProgress("PLATFORM:   " + pf_system + "\n");
        publishProgress("* VENDOR:   " + pf_system_vendor + "\n");
        publishProgress("* MODEL:   " + pf_system_model + "\n");
        publishProgress("OS:   " + pf_os + "\n");
        publishProgress("* SHORT:   " + pf_os_short + "\n");
        publishProgress("* LONG:   " + pf_os_long + "\n");
        publishProgress("* BITS:   " + pf_os_bits + "\n");
        publishProgress("MEMORY:   " + pf_memory + "\n");
        publishProgress("CPU:   " + pf_cpu + "\n");
        publishProgress("* SUBNAME:   " + pf_cpu_subname + "\n");
        publishProgress("* ABI:   " + pf_cpu_abi + "\n");
        publishProgress("* FEATURES:   " + pf_cpu_features + "\n");
        publishProgress("* CORES:   " + pf_cpu_num + "\n");
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

            publishProgress(x + "\n");
        }
        publishProgress("GPU:   " + pf_gpu + "\n");
        publishProgress("* VENDOR:   " + pf_gpu_vendor + "\n");
        publishProgress("* OPENCL:   " + pf_gpu_opencl + "\n");

        //Delay program for 1 sec
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
        }

        //Communicate with CK **************************************************
        JSONObject j_os, j_cpu, j_gpu, j_sys;

        String j_os_uid = "";
        String j_cpu_uid = "";
        String j_gpu_uid = "";
        String j_sys_uid = "";

//            publishProgress(s_line);
        publishProgress("Exchanging info about your platform with CK server to retrieve latest meta for crowdtuning ...");

        requestObject = new JSONObject();
        JSONObject platformFeatures = new JSONObject();

        // OS ******
        publishProgress("\n    Exchanging OS info ...\n");

        try {
            ft_os = new JSONObject();

            ft_os.put("name", pf_os);
            ft_os.put("name_long", pf_os_long);
            ft_os.put("name_short", pf_os_short);
            ft_os.put("bits", pf_os_bits);

            platformFeatures.put("features", ft_os);

            if (getCurl() == null) {
                publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", getCurl());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform.os");
            requestObject.put("data_name", pf_os);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            publishProgress("\nError with JSONObject ...\n\n");
            return null;
        }

        j_os = exchange_info_with_ck_server(requestObject);
        int rr = 0;
        try {
            Object rx = j_os.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_os.get("error");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            publishProgress("\nProblem accessing CK server: " + err + "\n");
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
                publishProgress(x);
            }
        }

        // GPU ******
        if (!pf_gpu.equals("") && getCurl() != null) {
            publishProgress("    Exchanging GPU info ...\n");

            try {
                ft_gpu = new JSONObject();

                ft_gpu.put("name", pf_gpu);
                ft_gpu.put("vendor", pf_gpu_vendor);

                platformFeatures.put("features", ft_gpu);

                requestObject.put("remote_server_url", getCurl());//
                requestObject.put("action", "exchange");
                requestObject.put("module_uoa", "platform");
                requestObject.put("sub_module_uoa", "platform.gpu");
                requestObject.put("data_name", pf_gpu);
                requestObject.put("repo_uoa", repo_uoa);
                requestObject.put("all", "yes");
                requestObject.put("dict", platformFeatures);
                requestObject.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            j_gpu = exchange_info_with_ck_server(requestObject);
            try {
                Object rx = j_gpu.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) j_gpu.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
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
                    publishProgress(x);
                }
            }
        }

        // CPU ******
        publishProgress("    Exchanging CPU info ...\n");

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

            if (getCurl() == null) {
                publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", getCurl());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform.cpu");
            requestObject.put("data_name", pf_cpu);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            publishProgress("\nError with JSONObject ...\n\n");
            return null;
        }

        j_cpu = exchange_info_with_ck_server(requestObject);
        try {
            Object rx = j_cpu.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_cpu.get("error");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            publishProgress("\nProblem accessing CK server: " + err + "\n");
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
                publishProgress(x);
            }
        }

        // Platform ******
        publishProgress("    Exchanging platform info ...\n");

        try {
            ft_plat = new JSONObject();

            ft_plat.put("name", pf_system);
            ft_plat.put("vendor", pf_system_vendor);
            ft_plat.put("model", pf_system_model);

            platformFeatures.put("features", ft_plat);

            if (getCurl() == null) {
                publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            requestObject.put("remote_server_url", getCurl());//
            requestObject.put("action", "exchange");
            requestObject.put("module_uoa", "platform");
            requestObject.put("sub_module_uoa", "platform");
            requestObject.put("data_name", pf_system);
            requestObject.put("repo_uoa", repo_uoa);
            requestObject.put("all", "yes");
            requestObject.put("dict", platformFeatures);
            requestObject.put("out", "json");
        } catch (JSONException e) {
            publishProgress("\nError with JSONObject ...\n\n");
            return null;
        }

        j_sys = exchange_info_with_ck_server(requestObject);
        try {
            Object rx = j_sys.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) j_sys.get("error");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            publishProgress("\nProblem accessing CK server: " + err + "\n");
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
                publishProgress(x);
            }
        }

        //Delay program for 1 sec
        try {
            sleep(1000);
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
            publishProgress("\nError with platformFeatures ...\n\n");
            return null;
        }

        // Sending request to CK server to obtain available scenarios
             /*######################################################################################################*/
        publishProgress("\n    Sending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...\n\n");



        JSONObject availableScenariosRequest = new JSONObject();


        try {
            if (getCurl() == null) {
                publishProgress("\n Error we could not load scenarios from Collective Knowledge server: it's not reachible ...\n\n");
                return null;
            }
            availableScenariosRequest.put("remote_server_url", getCurl());
            availableScenariosRequest.put("action", "get");
            availableScenariosRequest.put("module_uoa", "experiment.scenario.mobile");
            availableScenariosRequest.put("email", AppConfigService.getEmail());
            availableScenariosRequest.put("platform_features", platformFeatures);
            availableScenariosRequest.put("out", "json");
        } catch (JSONException e) {
            publishProgress("\nError with JSONObject ...\n\n");
            return null;
        }

        try {
            r = openme.remote_access(availableScenariosRequest);
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        if (validateReturnCode(r)) return null;
        JSONObject scenariosJSON = r;
        try {
            openme.openme_store_json_file(scenariosJSON, cachedScenariosFilePath);
        } catch (JSONException e) {
            publishProgress("\nError writing preloaded scenarios to file (" + e.getMessage() + ") ...\n\n");
        }


        try {
            JSONArray scenarios = r.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
                return null;
            }

            File externalSDCardFile = new File(externalSDCardOpensciencePath);
            if (!externalSDCardFile.exists()) {
                if (!externalSDCardFile.mkdirs()) {
                    publishProgress("\nError creating dir (" + externalSDCardOpensciencePath + ") ...\n\n");
                    return null;
                }
            }

            String libPath = null;
            String executablePath = null;
            String defaultImageFilePath = null;
            if (scenarios.length() == 0) {
                publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
                return null;
            }

            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);
                final String module_uoa = scenario.getString("module_uoa");
                final String dataUID = scenario.getString("data_uid");
                final String data_uoa = scenario.getString("data_uoa");

                scenario.getJSONObject("search_dict");
                scenario.getString("ignore_update");
                scenario.getString("search_string");
                JSONObject meta = scenario.getJSONObject("meta");
                String title = meta.getString("title");
                Long sizeBytes = Long.valueOf(0);
                String sizeMB = "";
                try {
                    String sizeB = scenario.getString("total_file_size");
                    sizeBytes = Long.valueOf(sizeB);
                    sizeMB = Utils.bytesIntoHumanReadable(Long.valueOf(sizeB));
                } catch (JSONException e) {
                    publishProgress("Warn loading scenarios from file " + e.getLocalizedMessage());
                }

//                if (!isPreloadRunning &&
//                        (getSelectedRecognitionScenario() == null || !getSelectedRecognitionScenario().getTitle().equalsIgnoreCase(title))) {
//                    continue;
//                }

//                JSONArray files = meta.getJSONArray("files");
//                for (int j = 0; j < files.length(); j++) {
//                    JSONObject file = files.getJSONObject(j);
//                    String fileName = file.getString("filename");
//                    String fileDir = externalSDCardOpensciencePath + file.getString("path");
//                    File fp = new File(fileDir);
//                    if (!fp.exists()) {
//                        if (!fp.mkdirs()) {
//                            publishProgress("\nError creating dir (" + fileDir + ") ...\n\n");
//                            return null;
//                        }
//                    }
//
//                    final String targetFilePath = fileDir + File.separator + fileName;
//                    String finalTargetFilePath = targetFilePath;
//                    String finalTargetFileDir = fileDir;
//                    String url = file.getString("url");
//                    String md5 = file.getString("md5");
//
//                    if (!isPreloadMode && downloadFileAndCheckMd5(
//                            url,
//                            targetFilePath,
//                            md5,
//                            new MainActivity.ProgressPublisher() {
//                                @Override
//                                public void publish(int percent) {
//                                    String str="";
//
//                                    if (percent<0) str+="\n * Downloading file " + targetFilePath + " ...\n";
//                                    else  str+="  * "+percent+"%\n";
//
//                                    publishProgress(str);
//                                }
//
//                                @Override
//                                public void println(String text) {
//                                    publishProgress("\n" + text + "\n");
//                                }
//                            })) {
//                        String copyToAppSpace = null;
//                        try {
//                            copyToAppSpace = file.getString("copy_to_app_space");
//                        } catch (JSONException e) {
//                            // copyToAppSpace is not mandatory
//                        }
//                        if (copyToAppSpace != null && copyToAppSpace.equalsIgnoreCase("yes")) {
//                            String fileAppDir = localAppPath + file.getString("path");
//                            File appfp = new File(fileAppDir);
//                            if (!appfp.exists()) {
//                                if (!appfp.mkdirs()) {
//                                    publishProgress("\nError creating dir (" + fileAppDir + ") ...\n\n");
//                                    return null;
//                                }
//                            }
//
//                            final String targetAppFilePath = fileAppDir + File.separator + fileName;
//                            try {
//                                copy_bin_file(targetFilePath, targetAppFilePath);
//                                finalTargetFileDir = fileAppDir;
//                                finalTargetFilePath = targetAppFilePath;
//                                publishProgress("\n * File " + targetFilePath + " sucessfully copied to " + targetAppFilePath + "\n\n");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                publishProgress("\nError copying file " + targetFilePath + " to " + targetAppFilePath + " ...\n\n");
//                                return null;
//                            }
//
//                        }
//
//                        String executable = null;
//                        String library = null;
//                        try {
//                            executable = file.getString("executable");
//                            library = file.getString("library");
//                        } catch (JSONException e) {
//                            // executable is not mandatory
//                        }
//                        if (executable != null && executable.equalsIgnoreCase("yes")) {
//                            if (library != null && library.equalsIgnoreCase("yes")) {
//                                libPath = finalTargetFileDir;
//                            } else {
//                                executablePath = finalTargetFileDir;
//                            }
//                            String[] chmodResult = openme.openme_run_program(chmod744 + " " + finalTargetFilePath, null, finalTargetFileDir);
//                            if (chmodResult[0].isEmpty() && chmodResult[1].isEmpty() && chmodResult[2].isEmpty()) {
//                                publishProgress(" * File " + finalTargetFilePath + " sucessfully set as executable ...\n");
//                            } else {
//                                publishProgress("\nError setting  file " + targetFilePath + " as executable ...\n\n");
//                                return null;
//                            }
//                        }
//                    }
//
//                    String default_image = null;
//                    try {
//                        default_image = file.getString("default_image");
//                    } catch (JSONException e) {
//                        // executable is not mandatory
//                    }
//                    if (default_image != null && default_image.equalsIgnoreCase("yes")) {
//                        defaultImageFilePath = finalTargetFilePath;
//                    }
//                }
//
//                String actualImageFilePath = AppConfigService.getActualImagePath();
//                if (actualImageFilePath == null) {
//                    actualImageFilePath = defaultImageFilePath;
//                    AppConfigService.updateActualImagePath(actualImageFilePath);
//                }

                recognitionScenario.setModuleUOA(module_uoa);
                recognitionScenario.setDataUOA(data_uoa);
                recognitionScenario.setRawJSON(scenario);
                recognitionScenario.setDefaultImagePath(defaultImageFilePath);
                recognitionScenario.setTitle(title);
                recognitionScenario.setTotalFileSize(sizeMB);
                recognitionScenario.setTotalFileSizeBytes(sizeBytes);
//                recognitionScenarios.add(recognitionScenario);

                publishProgress("\nPreloaded scenario info:  " + recognitionScenario.toString() + "\n\n");


            }

        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        //Delay program for 1 sec
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
        }

//            publishProgress(s_line);
        publishProgress("Finished pre-loading shared scenarios for crowdsourcing!\n\n");
        publishProgress("Crowd engine is READY!\n");

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    spinnerAdapter.sort(SpinAdapter.COMPARATOR);
//                    spinnerAdapter.notifyDataSetChanged();
//                }
//            });
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        return null;
    }

    private String getCurl() {
        /*********** Obtaining CK server **************/
        publishProgress("\n"); //s_line);
        publishProgress("Obtaining list of public Collective Knowledge servers from " + url_cserver + " ...\n");
        String curlCached = get_shared_computing_resource(url_cserver);
        AppConfigService.updateRemoteServerURL(curlCached);
        return curlCached;
    }

    private boolean validateReturnCode(JSONObject r) {
        int rr = 0;
        if (!r.has("return")) {
            publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
            return true;
        }

        try {
            Object rx = r.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return true;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) r.get("error");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return true;
            }

            publishProgress("\nProblem accessing CK server: " + err + "\n");
            return true;
        }
        return false;
    }

    @NonNull
    private JSONObject getPlatformFeaturesJSONObject(String pf_gpu_openclx, JSONObject ft_cpu, JSONObject ft_os, JSONObject ft_gpu, JSONObject ft_plat, DeviceInfo deviceInfo) throws JSONException {
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


    class DeviceInfo {
        private String j_os_uid = "";
        private String j_cpu_uid = "";
        private String j_gpu_uid = "";
        private String j_sys_uid = "";

        public String getJ_os_uid() {
            return j_os_uid;
        }

        public void setJ_os_uid(String j_os_uid) {
            this.j_os_uid = j_os_uid;
        }

        public String getJ_cpu_uid() {
            return j_cpu_uid;
        }

        public void setJ_cpu_uid(String j_cpu_uid) {
            this.j_cpu_uid = j_cpu_uid;
        }

        public String getJ_gpu_uid() {
            return j_gpu_uid;
        }

        public void setJ_gpu_uid(String j_gpu_uid) {
            this.j_gpu_uid = j_gpu_uid;
        }

        public String getJ_sys_uid() {
            return j_sys_uid;
        }

        public void setJ_sys_uid(String j_sys_uid) {
            this.j_sys_uid = j_sys_uid;
        }
    }
}