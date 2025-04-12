import org.bouncycastle.math.ec.ECPoint;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import java.math.BigInteger;
import java.util.*;

public class Client {
    Curve25519 curve;

    private BigInteger c;

    private List<String> X;

    private Map<String, String> bHx2x;

    private List<MapEntry> xHx;

    public Client(){
        curve = SystemParams.curve25519;
    }


    public void loadX(String filePath){
        X = FileUtils.readFile(filePath);
    }

    public boolean validateServerData0_org(ServerData0 data0, List<ECPoint> thetaHy){
        BigInteger a_theta = data0.r;
        List<ECPoint> aHyList = data0.getaHy();

        for (int i = 0; i < thetaHy.size(); ++i){
            if (!aHyList.get(i).multiply(a_theta).equals(thetaHy.get(i))) {
                return false;
            }
        }

        return true;
    }
    public boolean validateServerData0(ServerData0 data0, List<ECPoint> thetaHy){
        BigInteger a_theta = data0.r;
        List<ECPoint> aHyList = data0.getaHy();

        // 使用并行流检查所有元素
        boolean isValid = IntStream.range(0, thetaHy.size())
                .parallel()  // 并行流
                .allMatch(i -> aHyList.get(i).multiply(a_theta).equals(thetaHy.get(i)));  // 每个元素都满足条件

        return isValid;
    }



    public ClientData0 prePareData0(ServerData0 serverData0){
        BigInteger b = curve.genRandomNumber();

        List<ECPoint> abHyList = new ArrayList<>();
        List<ECPoint> aHyList = serverData0.getaHy();

        abHyList = aHyList.parallelStream()
                .map(e -> e.multiply(b))  // 并行执行乘法操作
                .collect(Collectors.toList());  // 将结果收集到新的列表中

        //shuffle
        Random random = new Random();
        for (int i = abHyList.size() - 1; i >= 0; i--){
            int index = random.nextInt(0, i+1);

            ECPoint tmp1 = abHyList.get(index);
            abHyList.set(index, abHyList.get(i));
            abHyList.set(i, tmp1);
        }

        bHx2x = new HashMap<>();

        List<ECPoint> HxList = new ArrayList<>();
        X.parallelStream()
                .forEach(x -> {
                    ECPoint t = SystemParams.hashToPoint(x).multiply(b);
                    synchronized (HxList) {  // 确保对HxList的操作是线程安全的
                        HxList.add(t); // bH(X)
                    }
                    bHx2x.put(t.normalize().toString(), x);  // bHx2x是线程安全的，使用ConcurrentHashMap可以避免问题
                });

        return new ClientData0(HxList, abHyList);
    }

    public ClientData0 prePareData0_org(ServerData0 serverData0){
        BigInteger b = curve.genRandomNumber();

        List<ECPoint> abHyList = new ArrayList<>();
        List<ECPoint> aHyList = serverData0.getaHy();

        for (ECPoint e : aHyList){
            abHyList.add(e.multiply(b)); //b*aH(Y)
        }

        //shuffle
        Random random = new Random();
        for (int i = abHyList.size() - 1; i >= 0; i--){
            int index = random.nextInt(0, i+1);

            ECPoint tmp1 = abHyList.get(index);
            abHyList.set(index, abHyList.get(i));
            abHyList.set(i, tmp1);
        }

        bHx2x = new HashMap<>();

        List<ECPoint> HxList = new ArrayList<>();
        for (String x : X){
            ECPoint t = SystemParams.hashToPoint(x).multiply(b);
            HxList.add(t); //bH(X)
            bHx2x.put(t.normalize().toString(), x);
        }

        return new ClientData0(HxList, abHyList);
    }


    public List<String> parseIntersection(List<ECPoint> intersection){
        List<String> result = new ArrayList<>();

        for (ECPoint e : intersection){
            result.add(bHx2x.get(e.normalize().toString()));
        }

        return result;
    }


    public List<ECPoint> prePareData1(){
        c = SystemParams.curve25519.genRandomNumber();

        xHx = new ArrayList<>();
        List<ECPoint> cHxList = new ArrayList<>();

        X.parallelStream()
                .forEach(x -> {
                    ECPoint t = SystemParams.hashToPoint(x).multiply(c);

                    synchronized (cHxList) {  // 确保对cHxList的操作是线程安全的
                        cHxList.add(t); // cH(X)
                    }

                    synchronized (xHx) {  // 确保对xHx的操作是线程安全的
                        xHx.add(new MapEntry(x, t));
                    }
                });


        Random random = new Random();

        //Shuffle
        for (int i = cHxList.size() - 1; i >= 0; i--){
            int index = random.nextInt(0, i+1);

            ECPoint tmp1 = cHxList.get(index);
            cHxList.set(index, cHxList.get(i));
            cHxList.set(i, tmp1);

            MapEntry tmp2 = xHx.get(index);
            xHx.set(index, xHx.get(i));
            xHx.set(i, tmp2);
        }

        return cHxList;
    }
    public List<ECPoint> prePareData1_org(){
        c = SystemParams.curve25519.genRandomNumber();

        xHx = new ArrayList<>();
        List<ECPoint> cHxList = new ArrayList<>();
        for (String x : X){
            ECPoint t = SystemParams.hashToPoint(x).multiply(c);
            cHxList.add(t); //cH(X)

            xHx.add(new MapEntry(x, t));
        }

        Random random = new Random();

        //Shuffle
        for (int i = cHxList.size() - 1; i >= 0; i--){
            int index = random.nextInt(0, i+1);

            ECPoint tmp1 = cHxList.get(index);
            cHxList.set(index, cHxList.get(i));
            cHxList.set(i, tmp1);

            MapEntry tmp2 = xHx.get(index);
            xHx.set(index, xHx.get(i));
            xHx.set(i, tmp2);
        }

        return cHxList;
    }


    public boolean validateServerData1(ServerData1 data1, List<ECPoint> thetaHy){
        List<ECPoint> dHyList = data1.getdHyList();
        BigInteger d_inv_theta = data1.getD_inv_theta();

        boolean isValid = IntStream.range(0, thetaHy.size())
                .parallel()  // 开启并行流
                .allMatch(i -> dHyList.get(i).multiply(d_inv_theta).equals(thetaHy.get(i)));  // 对每个元素进行检查

        return isValid;
    }

    public List<String> intersection(ServerData1 data1){

        List<ECPoint> dHyList = data1.getdHyList();
        List<ECPoint> dcHxList = data1.getDcHxList();

        BigInteger c_inv = c.modInverse(curve.getOrder());//c^-1
        List<ECPoint> dHxList = new ArrayList<>();

        IntStream.range(0, dcHxList.size())
                .parallel()  // 开启并行流
                .forEach(i -> {
                    ECPoint dhy = dcHxList.get(i).multiply(c_inv); // c^-1 * cdH(x) = dH(x)
                    synchronized (dHxList) {  // 确保对 dHxList 的操作是线程安全的
                        dHxList.add(dhy);
                    }
                    synchronized (xHx) {  // 确保对 xHx 的操作是线程安全的
                        xHx.get(i).setHx(dhy);
                    }
                });

        List<ECPoint> cc = new ArrayList<>();

        HashSet<ECPoint> bHy_Set = new HashSet<>(dHyList);


        // 使用并行流来处理dHxList中的每个元素
        dHxList.parallelStream()
                .filter(e -> bHy_Set.contains(e))  // 并行检查是否包含在bHy_Set中
                .forEach(e -> cc.add(e));  // 将符合条件的元素加入cc


        List<String> result = new ArrayList<>();

        for (ECPoint e : cc){
            for (MapEntry entry : xHx){
                if (e.equals(entry.getHx())){
                    result.add(entry.getX());
                    break;
                }
            }
        }
        return result;
    }
}
