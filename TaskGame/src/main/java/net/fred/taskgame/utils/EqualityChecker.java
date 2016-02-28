package net.fred.taskgame.utils;

public class EqualityChecker {
    public static synchronized boolean check(Object[] aArr, Object[] bArr) {
        // Array size test
        boolean res = aArr.length == bArr.length;

        // If arrays have the same length
        if (res) {
            for (int i = 0; i < aArr.length; i++) {
                Object a = aArr[i];
                Object b = bArr[i];

                // Content test on each element
                if (a != null) {
                    res = a.equals(b);
                } else if (b != null) {
                    res = false;
                }

                // Exit if not equals
                if (!res)
                    break;
            }
        }

        return res;
    }
}
