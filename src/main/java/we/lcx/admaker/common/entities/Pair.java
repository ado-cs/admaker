package we.lcx.admaker.common.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * Created by Lin Chenxiao on 2019-12-31
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pair<K, V> {
    private K key;
    private V value;
    public K getKey(Function<K, K> func) {
        if (key != null) return key;
        key = func.apply(null);
        return key;
    }
    public V getValue(Function<V, V> func) {
        if (value != null) return value;
        value = func.apply(null);
        return value;
    }
}
