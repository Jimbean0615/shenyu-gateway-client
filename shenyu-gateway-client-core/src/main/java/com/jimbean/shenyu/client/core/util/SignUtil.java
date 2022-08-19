package com.jimbean.shenyu.client.core.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

public class SignUtil {
    public static final String TIMESTAMP = "timestamp";
    public static final String PATH = "path";
    public static final String VERSION = "version";
    public static final String VERSION_VALUE = "1.0.0";

    public SignUtil() {
    }

    public static String generateSign(String secretKey, String path, String timestamp) throws IllegalAccessException {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalAccessException("secreKey can not be empty!");
        } else if (!StringUtils.hasText(path)) {
            throw new IllegalAccessException("path can not be empty!");
        } else if (!StringUtils.hasText(timestamp)) {
            throw new IllegalAccessException("timestamp can not be empty!");
        } else {
            Map<String, String> map = new HashMap();
            map.put("timestamp", timestamp);
            map.put("path", path);
            map.put("version", "1.0.0");
            List<String> storedKeys = (List) Arrays.stream(map.keySet().toArray(new String[0])).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            String sign = ((String) storedKeys.stream().map((key) -> {
                return String.join("", key, (CharSequence) map.get(key));
            }).collect(Collectors.joining())).trim().concat(secretKey);
            return DigestUtils.md5DigestAsHex(sign.getBytes()).toUpperCase();
        }
    }
}
