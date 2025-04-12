import org.bouncycastle.math.ec.ECPoint;

import javax.swing.text.html.StyleSheet;
import java.io.File;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        TimeCalculator time_cal = new TimeCalculator();

        String XPath = "D:\\Data\\client.txt";
        String YPath = "D:\\Data\\server.txt";
        String resultPath = "D:\\Data\\result.txt";

        Client client = new Client();
        Server server = new Server();

        client.loadX(XPath);

        server.loadY(YPath);

        List<ECPoint> evi = server.evidence();

        ServerData0 serverData0 = server.prePareData0();

        boolean verify_result = client.validateServerData0(serverData0, evi);

        if (verify_result){
            ClientData0 clientData0 = client.prePareData0(serverData0);
            List<ECPoint> intersection = server.intersection(clientData0);
            List<String> result1 = client.parseIntersection(intersection);
            System.out.println("X和Y的交集是：" + result1);

            List<ECPoint> cHxList = client.prePareData1();

            ServerData1 serverData1 = server.prepareData1(cHxList);

            verify_result = client.validateServerData1(serverData1, evi);
            if (verify_result){
                List<String> result2 = client.intersection(serverData1);
                System.out.println("X和Y的交集是：" + result2);

                result1.sort((s1, s2)->{return s1.compareTo(s2);});
                result2.sort((s1, s2)->{return s1.compareTo(s2);});
                boolean r = result1.equals(result2);
                if (r){
                    FileUtils.storeFile(resultPath, result1);
                    System.out.println("客户端验证成功：两轮交集一致");
                }
            }
            else {
                System.out.println("客户端验证失败：服务端发送的数据与存证数据不一致");
            }

        }
        else {
            System.out.println("客户端验证失败：服务端发送的数据与存证数据不一致");
        }
    }
}
