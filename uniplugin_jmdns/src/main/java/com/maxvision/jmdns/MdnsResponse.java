package com.maxvision.jmdns;

/**
 * user: zjj
 * date: 2024/6/19
 * desc: 描述
 */
public class MdnsResponse {

    public int code;
    public DataDTO data;
    public String requestId;
    public int status;
    public String timestamp;

    public static class DataDTO {
        public String mac;
        public String name;
        public String sn;
        public int type;

        @Override
        public String toString() {
            return "DataDTO{" +
                    "mac='" + mac + '\'' +
                    ", name='" + name + '\'' +
                    ", sn='" + sn + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MdnsResponse{" +
                "code=" + code +
                ", data=" + data +
                ", requestId='" + requestId + '\'' +
                ", status=" + status +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
