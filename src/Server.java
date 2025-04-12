import org.bouncycastle.math.ec.ECPoint;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import java.math.BigInteger;
import java.util.*;

public class Server {
    Curve25519 curve;
    private BigInteger theta;

    private BigInteger a;

    private List<String> Y;

    private Map<String, String> dHy2y;

    public Server(){
        curve = SystemParams.curve25519;
    }

    private List<ECPoint> hyList;

    public void loadY(String filePath){
        Y = FileUtils.readFile(filePath);
    }


    public List<ECPoint> evidence(){
        List<ECPoint> thetaHy = new ArrayList<>();

        theta = curve.genRandomNumber();

//        Hy2y = new HashMap<>();

        for (String s : Y){
            ECPoint point = SystemParams.hashToPoint(s);

//            Hy2y.put(point.normalize().normalize().toString(), s);

            ECPoint theta_hy = point.multiply(theta);
            thetaHy.add(theta_hy);
        }

        return thetaHy;
    }

    public ServerData0 prePareData0_org(){
        List<ECPoint> aHy = new ArrayList<>();

        a = curve.genRandomNumber();

        for (String s : Y){
            ECPoint point = SystemParams.hashToPoint(s);
            ECPoint a_hy = point.multiply(a);
            aHy.add(a_hy);
        }

        BigInteger r = a.modInverse(curve.getOrder()).multiply(theta).mod(curve.getOrder());

        return new ServerData0(aHy, r);
    }

    public ServerData0 prePareData0() {
        List<ECPoint> aHy = new ArrayList<>();

        a = curve.genRandomNumber();

        // 使用并行流来替代for循环
        aHy = Y.parallelStream()
                .map(s -> {
                    ECPoint point = SystemParams.hashToPoint(s);
                    return point.multiply(a);
                })
                .collect(Collectors.toList());

        BigInteger r = a.modInverse(curve.getOrder()).multiply(theta).mod(curve.getOrder());
        return new ServerData0(aHy, r);
    }



    public List<ECPoint> intersection_org(ClientData0 clientData0){

        List<ECPoint> abHyList = clientData0.getAbHy();
        List<ECPoint> bHx = clientData0.getbHx();

        //a^-1

        BigInteger a_invert = a.modInverse(curve.getOrder());

        List<ECPoint> bHy = new ArrayList<>();

        for (ECPoint e : abHyList){
            bHy.add(e.multiply(a_invert)); //a^-1 * abH(Y) = bH(Y)
        }


        List<ECPoint> cc = new ArrayList<>();

        HashSet<ECPoint> bHy_Set = new HashSet<>(bHy);

        for (ECPoint e : bHx) {
            if (bHy_Set.contains(e))
                cc.add(e);
        }
        return cc;
    }
    public List<ECPoint> intersection(ClientData0 clientData0){

        List<ECPoint> abHyList = clientData0.getAbHy();
        List<ECPoint> bHx = clientData0.getbHx();

        //a^-1

        BigInteger a_invert = a.modInverse(curve.getOrder());

        List<ECPoint> bHy = new ArrayList<>();
        bHy = abHyList.parallelStream()
                .map(e -> e.multiply(a_invert))  // 并行执行乘法操作
                .collect(Collectors.toList());  // 将结果收集到新的列表中


        List<ECPoint> cc = new ArrayList<>();

        HashSet<ECPoint> bHy_Set = new HashSet<>(bHy);

        cc = bHx.parallelStream().filter(e -> bHy_Set.contains(e)).collect(Collectors.toList());  // 将符合条件的元素收集到cc中
        return cc;
    }



    public ServerData1 prepareData1(List<ECPoint> cHxList){
        final BigInteger d = SystemParams.curve25519.genRandomNumber();
        final BigInteger order = curve.getOrder();

        List<ECPoint> dHyList = Y.parallelStream()
                .map(y -> SystemParams.hashToPoint(y).multiply(d))
                .collect(Collectors.toList()); // 自动线程安全且保留顺序

        // 处理cHxList（保留顺序）
        List<ECPoint> dcHxList = cHxList.parallelStream()
                .map(chx -> chx.multiply(d))
                .collect(Collectors.toList());

        // 后续计算
        BigInteger d_inv = d.modInverse(order);
        BigInteger d_inv_theta = d_inv.multiply(theta).mod(order);

        return new ServerData1(dHyList, d_inv_theta, dcHxList);
    }

    public ServerData1 prepareData1_org(List<ECPoint> cHxList){
        BigInteger d = SystemParams.curve25519.genRandomNumber();

        List<ECPoint> dHyList = new ArrayList<>();

        dHy2y = new HashMap<>();

        for (String y : Y){
            ECPoint e = SystemParams.hashToPoint(y).multiply(d);
            dHyList.add(e); //dH(y)
            dHy2y.put(e.normalize().toString(), y);
        }


        BigInteger d_inv = d.modInverse(curve.getOrder());
        BigInteger d_inv_theta = d_inv.multiply(theta).mod(curve.getOrder());

        List<ECPoint> dcHxList = new ArrayList<>();

        dcHxList = cHxList.parallelStream()
                .map(chx -> chx.multiply(d))  // 并行执行乘法操作
                .collect(Collectors.toList());  // 将结果收集到新的列表中

        return new ServerData1(dHyList, d_inv_theta, dcHxList);
    }


}
