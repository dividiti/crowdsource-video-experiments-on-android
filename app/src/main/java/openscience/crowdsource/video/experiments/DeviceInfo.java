package openscience.crowdsource.video.experiments;

/**
 * Bean with device details used for caching and pass basic device info
 *
 * @author Daniil Efremov
 */
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
