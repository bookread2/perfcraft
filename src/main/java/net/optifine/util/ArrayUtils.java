package net.optifine.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayUtils {
    public static boolean contains(Object[] arr, Object val) {
        if (arr == null) {
            return false;
        } else {
            for (int i = 0; i < arr.length; i++) {
                Object obj = arr[i];
                if (obj == val) {
                    return true;
                }
            }

            return false;
        }
    }

    public static int[] addIntsToArray(int[] intArray, int[] copyFrom) {
        if (intArray != null && copyFrom != null) {
            int arrLen = intArray.length;
            int newLen = arrLen + copyFrom.length;
            int[] newArray = new int[newLen];
            System.arraycopy(intArray, 0, newArray, 0, arrLen);

            for (int index = 0; index < copyFrom.length; index++) {
                newArray[index + arrLen] = copyFrom[index];
            }

            return newArray;
        } else {
            throw new NullPointerException("The given array is NULL");
        }
    }

    public static int[] addIntToArray(int[] intArray, int intValue) {
        return addIntsToArray(intArray, new int[]{intValue});
    }

    public static Object[] addObjectsToArray(Object[] arr, Object[] objs) {
        if (arr == null) {
            throw new NullPointerException("The given array is NULL");
        } else if (objs.length == 0) {
            return arr;
        } else {
            int arrLen = arr.length;
            int newLen = arrLen + objs.length;
            Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), newLen);
            System.arraycopy(arr, 0, newArr, 0, arrLen);
            System.arraycopy(objs, 0, newArr, arrLen, objs.length);
            return newArr;
        }
    }

    public static Object[] addObjectToArray(Object[] arr, Object obj) {
        if (arr == null) {
            throw new NullPointerException("The given array is NULL");
        } else {
            int arrLen = arr.length;
            int newLen = arrLen + 1;
            Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), newLen);
            System.arraycopy(arr, 0, newArr, 0, arrLen);
            newArr[arrLen] = obj;
            return newArr;
        }
    }

    public static Object[] addObjectToArray(Object[] arr, Object obj, int index) {
        List list = new ArrayList<>(Arrays.asList(arr));
        list.add(index, obj);
        Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), list.size());
        return list.toArray(newArr);
    }

    public static String arrayToString(boolean[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                boolean x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(float[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(float[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                float x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(float[] arr, String separator, String format) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                float x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.format(format, x));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(int[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(int[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                int x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static String arrayToHexString(int[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                int x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append("0x");
                buf.append(Integer.toHexString(x));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(Object[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(Object[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                Object obj = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(obj));
            }

            return buf.toString();
        }
    }

    public static Object[] collectionToArray(Collection coll, Class elementClass) {
        if (coll == null) {
            return null;
        } else if (elementClass == null) {
            return null;
        } else if (elementClass.isPrimitive()) {
            throw new IllegalArgumentException("Can not make arrays with primitive elements (int, double), element class: " + elementClass);
        } else {
            Object[] array = (Object[])Array.newInstance(elementClass, coll.size());
            return coll.toArray(array);
        }
    }

    public static boolean equalsOne(int val, int[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == val) {
                return true;
            }
        }

        return false;
    }

    public static boolean equalsOne(Object a, Object[] bs) {
        if (bs == null) {
            return false;
        } else {
            for (int i = 0; i < bs.length; i++) {
                Object b = bs[i];
                if (equals(a, b)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else {
            return o1 == null ? false : o1.equals(o2);
        }
    }

    public static boolean isSameOne(Object a, Object[] bs) {
        if (bs == null) {
            return false;
        } else {
            for (int i = 0; i < bs.length; i++) {
                Object b = bs[i];
                if (a == b) {
                    return true;
                }
            }

            return false;
        }
    }

    public static Object[] removeObjectFromArray(Object[] arr, Object obj) {
        List list = new ArrayList<>(Arrays.asList(arr));
        list.remove(obj);
        return collectionToArray(list, arr.getClass().getComponentType());
    }

    public static int[] toPrimitive(Integer[] arr) {
        if (arr == null) {
            return null;
        } else if (arr.length == 0) {
            return new int[0];
        } else {
            int[] intArr = new int[arr.length];

            for (int i = 0; i < intArr.length; i++) {
                intArr[i] = arr[i];
            }

            return intArr;
        }
    }
}
