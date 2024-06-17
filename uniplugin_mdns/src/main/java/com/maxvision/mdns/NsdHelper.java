package com.maxvision.mdns;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * user: zjj
 * date: 2024/6/7
 * desc: nsd帮助管理类
 */
public class NsdHelper {

    private static final String TAG = "zjj_mdns";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private boolean discoveryStarted = false;
    private boolean resolveInProgress = false;
    private final Queue<NsdServiceInfo> serviceQueue;
    private final List<NsdBean> resolvedServices;
    private ResolveResultListener resultListener;
    private int discoveredServicesCount = 0;
    private String serviceType = "_maxvision._tcp.";
    private final HashSet<String> resolvedServiceSet; // 用于存储已解析服务的唯一标识符
    private NsdBean nsdBean;

    public void setResultListener(ResolveResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public interface ResolveResultListener {
        void onAllServicesResolved(List<NsdBean> services);

    }

    public NsdHelper(Context context) {
        this(context, null);
    }

    public NsdHelper(Context context, ResolveResultListener listener) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        resultListener = listener;
        serviceQueue = new LinkedList<>();
        resolvedServices = new ArrayList<>();
        resolvedServiceSet = new HashSet<>(); // 初始化 HashSet
        initializeDiscoveryListener();
    }

    public void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.e(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.e(TAG, "Service discovery success: " + service);
                if (!service.getServiceType().startsWith(serviceType)) {
                    Log.e(TAG, "Unknown Service Type: " + service.getServiceType());
                } else {
                    serviceQueue.add(service);
                    discoveredServicesCount++;
                    if (!resolveInProgress) {
                        resolveNextService();
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
                discoveryStarted = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
                discoveryStarted = false;
            }
        };
    }

    private synchronized void resolveNextService() {
        if (serviceQueue.isEmpty()) {
            resolveInProgress = false;
            if (discoveredServicesCount > 0 && resolvedServices.size() == discoveredServicesCount) {
                if (resultListener != null) {
                    resultListener.onAllServicesResolved(new ArrayList<>(resolvedServices));
                }
            }
            return;
        }
        resolveInProgress = true;
        NsdServiceInfo serviceInfo = serviceQueue.poll();
        NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
                resolveInProgress = false;
                resolveNextService();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                String host = serviceInfo.getHost().getHostAddress();
                int port = serviceInfo.getPort();
                Log.d(TAG, "Host: " + host + ", Port: " + port);
                // 这里可以进一步处理解析到的服务，例如连接到服务
                resolveInProgress = false;
                String uniqueServiceIdentifier = serviceInfo.getServiceName() + serviceInfo.getHost().getHostAddress();
                if (!resolvedServiceSet.contains(uniqueServiceIdentifier)) {
                    resolvedServiceSet.add(uniqueServiceIdentifier);
                    nsdBean = new NsdBean();
                    if (host != null) {
                        nsdBean.ipAddress = host.replace("/", "");
                    }
                    nsdBean.port = port;
                    nsdBean.serviceType = serviceType;
                    nsdBean.serviceName = serviceInfo.getServiceName();
                    nsdBean.attributes = new NsdBean.AttributesDTO();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (!serviceInfo.getAttributes().isEmpty()) {
                            Map<String, byte[]> attributes = serviceInfo.getAttributes();
                            for (Map.Entry<String, byte[]> entry : attributes.entrySet()) {
                                String key = entry.getKey();
                                byte[] value = entry.getValue();
                                Log.e("result", "key:" + key + ",value:" + value);
                                if ("mv_sn".equals(key) && value != null) {
                                    nsdBean.attributes.mv_sn = new String(value);
                                }
                                if ("mv_type".equals(key) && value != null) {
                                    nsdBean.attributes.mv_type = new String(value);
                                }
                            }
                        }
                    }
                    Log.e(TAG, "组装后的数据:" + nsdBean.toString());
                    resolvedServices.add(nsdBean);
                } else {
                    Log.d(TAG, "Duplicate service ignored: " + serviceInfo.getServiceName());
                }
                resolveNextService();
            }
        };
        nsdManager.resolveService(serviceInfo, resolveListener);
    }

    public void discoverServices(String serviceType) {
        this.serviceType = serviceType;
        if (!discoveryStarted) {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            discoveryStarted = true;
        } else {
            Log.e(TAG, "Discovery already started");
        }
    }

    public void stopDiscovery() {
        if (discoveryStarted) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
                resolvedServiceSet.clear();
                resolvedServices.clear();
                discoveredServicesCount = 0;
                discoveryStarted = false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "No discovery to stop");
            }
        }
    }

    public void release() {
        if (null != nsdManager) {
            discoveryStarted = false;
            resolveInProgress = false;
            nsdManager = null;
        }
    }


}
