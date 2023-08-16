package com.paranoiaworks.unicus.android.sse.config;

import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

/**
 * Configuration that requires some "Current State / System Depended Adjustment"
 *
 * @author Paranoia Works
 * @version 1.0.0
 */
public class DynamicConfig {

    private static final int MAX_CTR_PARALLELIZATION_NC = 4; // 1, 2, 4, 8, ...
    private static final int MAX_CTR_PARALLELIZATION_PI = 1;

    public static int getCTRParallelizationPI()
    {
        Integer ctrParallelizationPI = (Integer)SettingDataHolder.getSessionCacheObject("CTRParallelizationPI");
        if(ctrParallelizationPI != null) return ctrParallelizationPI;
        else {
            int cores = getNumberOfCPUCores();
            if(cores > 6) ctrParallelizationPI = 4;
            else if(cores >= 4) ctrParallelizationPI = 2;
            else ctrParallelizationPI = 1;
            if(MAX_CTR_PARALLELIZATION_PI < ctrParallelizationPI) ctrParallelizationPI = MAX_CTR_PARALLELIZATION_PI;
            SettingDataHolder.addOrReplaceSessionCacheObject("CTRParallelizationPI", ctrParallelizationPI);
            return ctrParallelizationPI;
        }
    }

    public static int getCTRParallelizationNC()
    {
        Integer ctrParallelizationNC = (Integer)SettingDataHolder.getSessionCacheObject("CTRParallelizationNC");
        if(ctrParallelizationNC != null) return ctrParallelizationNC;
        else {
            int cores = getNumberOfCPUCores();
            if(cores > 6) ctrParallelizationNC = 4;
            else if(cores >= 4) ctrParallelizationNC = 2;
            else ctrParallelizationNC = 1;
            if(MAX_CTR_PARALLELIZATION_NC < ctrParallelizationNC) ctrParallelizationNC = MAX_CTR_PARALLELIZATION_NC;
            SettingDataHolder.addOrReplaceSessionCacheObject("CTRParallelizationNC", ctrParallelizationNC);
            return ctrParallelizationNC;
        }
    }

    public static int getNumberOfCPUCores()
    {
        int cores = 1;
        Integer coresCached = (Integer)SettingDataHolder.getSessionCacheObject("CPU_CORES");

        if (coresCached != null) {
            cores = coresCached;
        }
        else {
            try {
                cores = Helpers.getNumberOfCores();
                SettingDataHolder.addOrReplaceSessionCacheObject("CPU_CORES", cores);
            } catch (Exception e) {
                // N/A
            }
        }
        return cores;
    }
}
