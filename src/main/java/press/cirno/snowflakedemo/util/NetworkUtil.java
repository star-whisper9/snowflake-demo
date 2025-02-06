package press.cirno.snowflakedemo.util;

import lombok.extern.slf4j.Slf4j;
import press.cirno.snowflakedemo.exception.NetworkUtilException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

@Slf4j
public class NetworkUtil {
    /**
     * 获取部署机器的第一个合法网卡的 IP 和 MAC<br >
     * 合法网卡：非回环、非虚拟、已启用，绑定 IP 非环回非广播（但允许本地链路）的网卡<br >
     *
     * @return IP 和 MAC 为两个元素的列表 || null
     */
    public static List<String> getLocalNetworkInfo() {
        try {
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (interfaceEnumeration.hasMoreElements()) {
                NetworkInterface networkInterface = interfaceEnumeration.nextElement();
                // 排除回环、虚拟、未启用的网卡
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                // 获取 IP
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                while (inetAddressEnumeration.hasMoreElements()) {
                    InetAddress inetAddress = inetAddressEnumeration.nextElement();
                    if (inetAddress.isLoopbackAddress()
                            // || inetAddress.isLinkLocalAddress()
                            || inetAddress.isMulticastAddress()) {
                        continue;
                    }
                    // 判断地址是否是 v6
                    String ip = inetAddress.getHostAddress();
                    if (ip.contains(":")) {
                        continue;
                    }
                    String mac = getMacByIa(inetAddress);
                    if (mac == null) {
                        continue;
                    }
                    return List.of(ip, mac);
                }
            }

            log.error("未找到可用的网卡信息");
            return null;
        } catch (SocketException e) {
            log.error("获取网卡信息失败", e);
            return null;
        } catch (NetworkUtilException e) {
            log.error("获取 MAC 地址失败", e);
            return null;
        }
    }

    /**
     * 通过 InetAddress 获取 MAC 地址
     *
     * @param ia InetAddress
     * @return 减号分隔的大写 MAC 地址
     * @throws SocketException      获取 MAC 地址 / 获取网络接口失败
     * @throws NetworkUtilException 获取到的 MAC 地址为空
     */
    private static String getMacByIa(InetAddress ia) throws SocketException {
        byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        if (mac == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) {
            sb.append(String.format("%02X", b));
            sb.append("-");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
