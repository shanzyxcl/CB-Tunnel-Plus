package com.cbtunnel.plus.config;

public interface SettingsConstants
{
    public static final String UDP_CLI = "{\n  \"server\": \"%s\",\n  \"obfs\": \"%s\",\n  \"auth_str\": \"%s\",\n  \"up_mbps\": %s,\n  \"down_mbps\": %s,\n  \"retry\": %s,\n  \"retry_interval\": %s,\n  \"socks5\": {\n    \"listen\": \"%s\"\n  },\n  \"http\": {\n    \"listen\": \"%s\"\n  },\n  \"insecure\": %s,\n  \"lazy_start\": true,\n  \"ca\": \"%s\",\n  \"recv_window_conn\": %s,\n \"recv_window\": %s\n}";

    public static final String
            user1 = "debian",
            user2 = "mhixaccount",
            user3 = "test123",
            pass3 = "12345",
            manual_tunnel_radio_key = "manual_tunnel_radio_key",
            network_spin_mSelection_key = "network_spin_mSelection_key",
            server_spin_mSelection_key = "server_spin_mSelection_key",
            CONFIG_EXP_KEY = "_CONFIG_EXP_KEY",
            SERVER_WEB_RENEW_KEY = "_SERVER_WEB_RENEW_KEY",
            THEME_UTIL_KEY = "_THEME_UTIL_KEY",
            IS_AUTO_LOGIN = "_IS_AUTO_LOGIN",
            CONFIG_PASSCODE_KEY = "_CONFIG_PASSCODE_KEY",
            ISAUTO_REPLACE = "_ISAUTO_REPLACE",
            PAUSE_VPN_ON_BLANKED_SCREEN_KEY = "pause_vpn_on_blanked_screen",
            AUTO_CLEAR_LOGS_KEY = "_AUTO_CLEAR_LOGS_KEY",
            AUTO_RECONN_TIME_KEY = "-AUTO_RECONN_TIME_KEY",
            TETHERING_SUBNET = "_TETHERING_SUBNET",
            DISABLE_DELAY_KEY = "_DISABLE_DELAY_KEY",
            PREF_PROXY_ADDRESS_KEY = "pref_vpn_proxy_address",
            DATA_COMPRESSION = "_DATA_COMPRESSION",
            FILTER_APPS = "FILTER_APPS_KEY",
            FILTER_BYPASS_MODE = "_FILTER_BYPASS_MODE_KEY",
            FILTER_APPS_LIST = "_FILTER_APPS_LIST_KEY",
            DIRECT_UDP_CONFIG_KEY = "_DIRECT_UDP_CONFIG_KEY",
            LOAD_OVPN_SSH_TWEAKS_KEY = "_LOAD_OVPN_SSH_TWEAKS_KEY",
            LOAD_DNS_TWEAKS_KEY = "_LOAD_DNS_TWEAKS_KEY",
            LOAD_V2RAY_TWEAKS_KEY = "_LOAD_V2RAY_TWEAKS_KEY",
            LOAD_UDP_TWEAKS_KEY = "_LOAD_UDP_TWEAKS_KEY";


    //DNS TYPE
    public static final String
            DNSFORWARD_KEY = "_DNSFORWARD_KEY",
            DNSRESOLVER_KEY = "_DNSRESOLVER_KEY",
            UDPFORWARD_KEY = "_UDPFORWARD_KEY",
            UDPRESOLVER_KEY = "_UDPRESOLVER_KEY",
            DNS_PUBLIC_KEY= "HRL_DNS_PUBLIC_KEY",
            DNS_ADDRESS_KEY = "HRL_DNS_ADDRESS_KEY",
            DNS_NAME_SERVER_KEY = "HRL_DNS_NAME_SERVER_KEY";
    // VPN
    public static final String
            SERVER_SOCKET_TYPE_KEY = "_SERVER_SOCKET_TYPE_KEY",
            SERVER_KEY = "_SERVER_KEY",
            SERVER_PORT_KEY = "_SERVER_PORT_KEY",
            PROXY_IP_KEY = "_PROXY_IP_KEY",
            PROXY_PORT_KEY = "_PROXY_PORT_KEY",
            USERNAME_KEY = "_USERNAME_KEY",
            PASSWORD_KEY = "_PASSWORD_KEY",
            PORTA_LOCAL_KEY = "_PORTA_LOCAL_KEY",
            PINGER_KEY = "_PINGER_KEY",
            PAYLOAD_TYPE_KEY = "_PAYLOAD_TYPE_KEY",
            CUSTOM_PAYLOAD_KEY = "_CUSTOM_PAYLOAD_KEY",
            SNI_V2RAY_KEY = "_SNI_V2RAY_KEY",
            SNI_HOST_KEY = "_SNI_HOST_KEY";

    // TYPE SECURE SHELL PAYLOAD TYPE
    public static final int
            PAYLOAD_TYPE_DIRECT = 1,
            PAYLOAD_TYPE_DIRECT_PAYLOAD = 2,
            PAYLOAD_TYPE_HTTP_PROXY = 3,
            PAYLOAD_TYPE_SSL = 4,
            PAYLOAD_TYPE_SSL_PAYLOAD = 5,
            PAYLOAD_TYPE_SSL_PROXY = 6,
            PAYLOAD_TYPE_OVPN_UDP = 8;

    // Stringer
    public static final String
            APP_COLORS = "_APP_COLORS",
            UPLOAD_POST_API = "_UPLOAD_POST_API",
            UPLOAD_GET_API = "_UPLOAD_GET_API",
            CONFIG_API = "_CONFIG_API",
            CONFIG_URL = "_CONFIG_URL",
            CONFIG_EDITOR_CODE = "CONFIG_EDITOR_CODE_",
            ISQUERY_MODE = "_ISQUERY_MODE",
            FRONT_QUERY = "_FRONT_QUERY",
            BACK_QUERY = "_BACK_QUERY",
            SERVER_TYPE = "HRL_SERVER_TYPE",
            CONFIG_VERSION = "HRL_CONFIG_VERSION",
            RELEASE_NOTE = "HRL_RELEASE_NOTE_KEY",
            CONTACT_SUPPORT = "7_CONTACT_SUPPORT",
            OPEN_VPN_CERT = "HRL_SINGLE_CERT",
            CONFIG_V2RAY = "HRL_CONFIG_V2RAY",
            SERVER_POSITION = "HRL_SERVER_POSITION",
            NETWORK_POSITION = "HRL_NETWORK_POSITION",
            SERVER_TYPE_OVPN = "OVPN-Tunnel:",
            SERVER_TYPE_SSH = "SSH-Tunnel:",
            SERVER_TYPE_DNS = "DNS-Tunnel:",
            SERVER_TYPE_V2RAY = "V2RAY-Tunnel:",
            SERVER_TYPE_UDP_HYSTERIA_V1 = "UDP-Tunnel:";

}
