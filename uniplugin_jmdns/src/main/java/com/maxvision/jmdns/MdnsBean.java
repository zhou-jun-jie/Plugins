package com.maxvision.jmdns;

/**
 * user: zjj
 * date: 2024/6/11
 * desc: 封装的实体类
 */
public class MdnsBean {

    public String status;
    public String ipAddress;
    public Integer port;
    public String serviceType;
    public String serviceName;
    public AttributesDTO attributes;
    public Integer discoveryId;

    public static class AttributesDTO {
        public String mv_sn;
        public int mv_type;
        public String reply;
        public String code;

        @Override
        public String toString() {
            return "AttributesDTO{" +
                    "mv_sn='" + mv_sn + '\'' +
                    ", mv_type='" + mv_type + '\'' +
                    ", reply='" + reply + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "NsdBean{" +
                "status='" + status + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", serviceType='" + serviceType + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", attributes=" + attributes +
                ", discoveryId=" + discoveryId +
                '}';
    }
}
