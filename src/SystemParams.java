import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.pqc.math.linearalgebra.BigIntUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SystemParams {
    public static Curve25519 curve25519 = new Curve25519();

    //哈希到点函数
    public static ECPoint hashToPoint(String data){
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");//对元素进行哈希
            messageDigest.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] md = messageDigest.digest();

            while (true) {
                BigInteger xInt = new BigInteger(md);
                ECFieldElement x, y;

                try{
                    x = curve25519.getCurve().fromBigInteger(xInt);//将BigInteger类型的xInt转换为曲线上的一个点的横坐标
                }catch (IllegalArgumentException e){
                    messageDigest.update(md);
                    md = messageDigest.digest();
                    continue;
                }

                y = x.square().add(curve25519.getCurve().getA()).multiply(x)//根据横坐标x，求y
                        .add(curve25519.getCurve().getB())
                        .sqrt();


                if (y == null) {//如果y不存在，重新对md哈希，再求y
                    messageDigest.update(md);
                    md = messageDigest.digest();
                    continue;
                }


                ECPoint ecPoint = curve25519.getCurve().createPoint(x.toBigInteger(), y.toBigInteger());//得到点(x,y)

                ecPoint = ecPoint.multiply(curve25519.getCurve().getCofactor());//乘以余因子，确保在曲线上

                if (ecPoint == null || !ecPoint.isValid()){
                    messageDigest.update(md);
                    md = messageDigest.digest();
                }

                return ecPoint;
            }

        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

}
