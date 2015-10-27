package org.zenframework.z8.server.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CollectionsUtil {

    private CollectionsUtil() {}

    public static boolean equals(Object o1, Object o2, String compareKey) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null && o2 == null || o1 == null && o2 != null)
            return false;
        if (o1 instanceof Map && o2 instanceof Map && !equals((Map<?, ?>) o1, (Map<?, ?>) o2, compareKey))
            return false;
        else if (o1 instanceof Collection && o2 instanceof Collection && !equals((List<?>) o1, (List<?>) o2, compareKey))
            return false;
        else
            return o1.equals(o2);
    }

    public static boolean equals(Map<?, ?> o1, Map<?, ?> o2, String compareKey) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null && o2 == null || o1 == null && o2 != null || o1.size() != o2.size())
            return false;
        for (Map.Entry<?, ?> entry : o1.entrySet()) {
            if (!equals(entry.getValue(), o2.get(entry.getKey()), compareKey))
                return false;
        }
        return true;
    }

    public static boolean equals(List<?> o1, List<?> o2, String compareKey) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null && o2 == null || o1 == null && o2 != null || o1.size() != o2.size())
            return false;
        Collections.sort(o1, new ElementComparator(compareKey));
        Collections.sort(o2, new ElementComparator(compareKey));
        for (int i = 0; i < o1.size(); i++) {
            if (!equals(o1.get(i), o2.get(i), compareKey))
                return false;
        }
        return true;
    }

    public static class ElementComparator implements Comparator<Object> {

        private final String compareKey;

        public ElementComparator(String compareKey) {
            this.compareKey = compareKey;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == null && o2 == null)
                return 0;
            if (o1 != null && o2 == null)
                return 1;
            if (o1 == null && o2 != null)
                return -1;
            return toString(o1).compareTo(toString(o2));
        }

        private String toString(Object o) {
            if (o instanceof Map) {
                try {
                    return "2" + toString(((Map<?, ?>) o).get(compareKey));
                } catch (NullPointerException e) {
                    throw new NullPointerException("Map " + o + " doesn't contain key '" + compareKey + "'");
                }
            }
            if (o instanceof List) {
                List<?> l = (List<?>) o;
                if (l.isEmpty())
                    return "3[]";
                Collections.sort(l, new ElementComparator(compareKey));
                return "3" + toString(l.get(0));
            }
            return "1" + o.toString();
        }

    }

}