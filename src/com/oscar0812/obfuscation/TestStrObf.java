package com.oscar0812.obfuscation;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;

public class TestStrObf {
    private final static HashMap<String, String> map = new HashMap<>();
    public static String callMethod(String methodName) {
        try {
            // reflection can get really slow, so cache
            if(map.containsKey(methodName)) {
                return map.get(methodName);
            }
            String sVar = (String) Class.forName("com.oscar0812.obfuscation.TestStrObf").getMethod(methodName).invoke(null);
            map.put(methodName, sVar);
            return sVar;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return "";
        }
    }

    public static String a() {
        int t;

        byte[] buf = new byte[28];
        // f=10, ff=255, a1=261120, a1n=-261121, byte=82, char=R, b1=83968, tr=833283623, t=833178151, c1=833094183
        t = 833178151;
        buf[0] = (byte) (t >>> 10);
        // f=17, ff=255, a1=33423360, a1n=-33423361, byte=101, char=e, b1=13238272, tr=-2028846231, t=-2033564823, c1=-2046803095
        t = -2033564823;
        buf[1] = (byte) (t >>> 17);
        // f=6, ff=255, a1=16320, a1n=-16321, byte=112, char=p, b1=7168, tr=-1906184081, t=-1906189265, c1=-1906196433
        t = -1906189265;
        buf[2] = (byte) (t >>> 6);
        // f=12, ff=255, a1=1044480, a1n=-1044481, byte=108, char=l, b1=442368, tr=1345875965, t=1345769469, c1=1345327101
        t = 1345769469;
        buf[3] = (byte) (t >>> 12);
        // f=8, ff=255, a1=65280, a1n=-65281, byte=97, char=a, b1=24832, tr=802842183, t=802840903, c1=802816071
        t = 802840903;
        buf[4] = (byte) (t >>> 8);
        // f=20, ff=255, a1=267386880, a1n=-267386881, byte=99, char=c, b1=103809024, tr=1771319743, t=1714696639, c1=1610887615
        t = 1714696639;
        buf[5] = (byte) (t >>> 20);
        // f=8, ff=255, a1=65280, a1n=-65281, byte=101, char=e, b1=25856, tr=343239579, t=343238043, c1=343212187
        t = 343238043;
        buf[6] = (byte) (t >>> 8);
        // f=2, ff=255, a1=1020, a1n=-1021, byte=32, char= , b1=128, tr=593100939, t=593100931, c1=593100803
        t = 593100931;
        buf[7] = (byte) (t >>> 2);
        // f=23, ff=255, a1=2139095040, a1n=-2139095041, byte=119, char=w, b1=998244352, tr=135810106, t=999836730, c1=1592378
        t = 999836730;
        buf[8] = (byte) (t >>> 23);
        // f=12, ff=255, a1=1044480, a1n=-1044481, byte=105, char=i, b1=430080, tr=1280935564, t=1280743052, c1=1280312972
        t = 1280743052;
        buf[9] = (byte) (t >>> 12);
        // f=4, ff=255, a1=4080, a1n=-4081, byte=116, char=t, b1=1856, tr=1306455993, t=1306453833, c1=1306451977
        t = 1306453833;
        buf[10] = (byte) (t >>> 4);
        // f=19, ff=255, a1=133693440, a1n=-133693441, byte=104, char=h, b1=54525952, tr=-1976168262, t=-1958342470, c1=-2012868422
        t = -1958342470;
        buf[11] = (byte) (t >>> 19);
        // f=11, ff=255, a1=522240, a1n=-522241, byte=32, char= , b1=65536, tr=86452119, t=86050711, c1=85985175
        t = 86050711;
        buf[12] = (byte) (t >>> 11);
        // f=18, ff=255, a1=66846720, a1n=-66846721, byte=121, char=y, b1=31719424, tr=-644414558, t=-639171678, c1=-670891102
        t = -639171678;
        buf[13] = (byte) (t >>> 18);
        // f=12, ff=255, a1=1044480, a1n=-1044481, byte=111, char=o, b1=454656, tr=998259618, t=998701986, c1=998247330
        t = 998701986;
        buf[14] = (byte) (t >>> 12);
        // f=5, ff=255, a1=8160, a1n=-8161, byte=117, char=u, b1=3744, tr=979654765, t=979652269, c1=979648525
        t = 979652269;
        buf[15] = (byte) (t >>> 5);
        // f=6, ff=255, a1=16320, a1n=-16321, byte=114, char=r, b1=7296, tr=-1612949352, t=-1612948328, c1=-1612955624
        t = -1612948328;
        buf[16] = (byte) (t >>> 6);
        // f=12, ff=255, a1=1044480, a1n=-1044481, byte=32, char= , b1=131072, tr=-354458086, t=-355334630, c1=-355465702
        t = -355334630;
        buf[17] = (byte) (t >>> 12);
        // f=6, ff=255, a1=16320, a1n=-16321, byte=111, char=o, b1=7104, tr=18334992, t=18340816, c1=18333712
        t = 18340816;
        buf[18] = (byte) (t >>> 6);
        // f=10, ff=255, a1=261120, a1n=-261121, byte=119, char=w, b1=121856, tr=-1163363571, t=-1163272435, c1=-1163394291
        t = -1163272435;
        buf[19] = (byte) (t >>> 10);
        // f=16, ff=255, a1=16711680, a1n=-16711681, byte=110, char=n, b1=7208960, tr=-188481382, t=-194117478, c1=-201326438
        t = -194117478;
        buf[20] = (byte) (t >>> 16);
        // f=14, ff=255, a1=4177920, a1n=-4177921, byte=32, char= , b1=524288, tr=1623591630, t=1623722702, c1=1623198414
        t = 1623722702;
        buf[21] = (byte) (t >>> 14);
        // f=8, ff=255, a1=65280, a1n=-65281, byte=97, char=a, b1=24832, tr=1633890597, t=1633902885, c1=1633878053
        t = 1633902885;
        buf[22] = (byte) (t >>> 8);
        // f=21, ff=255, a1=534773760, a1n=-534773761, byte=99, char=c, b1=207618048, tr=-708152311, t=-865438711, c1=-1073056759
        t = -865438711;
        buf[23] = (byte) (t >>> 21);
        // f=14, ff=255, a1=4177920, a1n=-4177921, byte=116, char=t, b1=1900544, tr=52593313, t=52232865, c1=50332321
        t = 52232865;
        buf[24] = (byte) (t >>> 14);
        // f=18, ff=255, a1=66846720, a1n=-66846721, byte=105, char=i, b1=27525120, tr=964797148, t=967156444, c1=939631324
        t = 967156444;
        buf[25] = (byte) (t >>> 18);
        // f=20, ff=255, a1=267386880, a1n=-267386881, byte=111, char=o, b1=116391936, tr=300440805, t=385375461, c1=268983525
        t = 385375461;
        buf[26] = (byte) (t >>> 20);
        // f=12, ff=255, a1=1044480, a1n=-1044481, byte=110, char=n, b1=450560, tr=39850834, t=40297298, c1=39846738
        t = 40297298;
        buf[27] = (byte) (t >>> 12);
        return new String(buf);
    }


    public static void main(String[] args) {
        String a = a();

        // i is the index. i=24
        // tr is random.                                                00000011001000101000001010100001,   52593313_10
        // f is random from (1, 24)                                     00000000000000000000000000001110,   14_10

        // a1 = (0xff << f) =>          255 << 14                       00000000001111111100000000000000,   4177920_10
        // 0000000011111111 << 14 =>    "left shift 255 by 14"          00000000001111111100000000000000,   4177920_10

        // a1n = ~a =>                  "Complement a", -(a+1)
        // a1n = ~4177920 =>            (4177920 + 1) = -4177921,       11111111110000000011111111111111,  -4177921_10

        // byte = 116, char=t=>         "byte is ascii code of char"    00000000000000000000000001110100,   116_10
        // b1 = b[i] << f               "left shift 116 by 14"          00000000000111010000000000000000,   1900544_10

        // c1 = tr & a1n;                "large random tr AND ~a"       00000011000000000000001010100001,   50332321_10

        // t = c1 | b1;                 "c1 OR b1"                      00000011000111010000001010100001,   52232865_10

        // buf[i] = (byte) (t >>> f);  "Unsigned right shift"           00000000000000000000000001110100,   116_10

        // pretty much shift bytes by f to the right (24 max shift since 32-8 = 24), and mess around with anything around those bytes.
        // for example, f=6, r=random bit, rrrrrrrrrrrrrrrrrr00000000rrrrrr
        // r can change to whatever as long as you don't touch the 0's
        // then override 0's with the char bytes, then shift r to the left
        // only get the last 8, all the other are random and don't matter

        System.out.println(a);
        args = new String[]{"Action"};
        Random r = new Random(System.currentTimeMillis());
        byte[] b = args[0].getBytes();
        int c = b.length;
        PrintStream o = System.out;

        o.println("(new Object() {");
        o.println("\tint t;");
        o.println("\tpublic String toString() {");
        o.print("\t\tbyte[] buf = new byte[");
        o.print(c);
        o.println("];");

        for (int i = 0; i < c; ++i) {
            int tr = r.nextInt();
            int f = r.nextInt(24) + 1; // >>>> max is 32 bits for some reason

            int a1, a1n, b1, c1, t;
            a1 = (0xff << f);
            a1n = ~a1;
            b1 = b[i] << f;
            c1 = tr & a1n;

            t = c1 | b1;

            // System.out.printf("\t\t// f=%d, ff=%d, a1=%d, a1n=%d, byte=%d, char=%s, b1=%d, tr=%d, t=%d, c1=%d\n", f, 0xff, a1, a1n, b[i], args[0].charAt(i), b1, tr, t, c1);
            // System.out.printf("\t\t// f=%s, ff=%s, a1=%s, a1n=%s, byte=%s, char=%s\n\t\t// b1=%s, tr=%s, t=%s, c1=%s\n", Integer.toBinaryString(f), Integer.toBinaryString(0xff), Integer.toBinaryString(a1), Integer.toBinaryString(a1n), Integer.toBinaryString(b[i]), args[0].charAt(i), Integer.toBinaryString(b1), Integer.toBinaryString(tr), Integer.toBinaryString(t), Integer.toBinaryString(c1));
            // System.out.printf("\t\t// %d_10, %s, %d_10, %s_2\n", (t>>>f), Integer.toBinaryString (t>>>f), (byte)(t>>>f), Integer.toBinaryString((byte)(t>>>f)));

            o.print("\t\tt = ");
            o.print(t);
            o.println(";");
            o.print("\t\tbuf[");
            o.print(i);
            o.print("] = (byte) (t >>> ");
            o.print(f);
            o.println(");");
        }

        o.println("\t\treturn new String(buf);");
        o.println("\t}\n}.toString())");
        o.println();


    }
}
