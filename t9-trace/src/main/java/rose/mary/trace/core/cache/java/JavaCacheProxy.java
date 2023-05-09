package rose.mary.trace.core.cache.java;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import rose.mary.trace.core.cache.CacheInfo;
import rose.mary.trace.core.cache.CacheProxy;

public class JavaCacheProxy<K, V> extends CacheProxy<K, V> {

    Map<K, V> cache = null;

    public JavaCacheProxy(String name, Map<K, V> cache) {
        super();
        super.name = name;
        this.cache = cache;
    }

    @Override
    public TransactionManager getTransactionManager() throws Exception {
        return null;
    }

    @Override
    public void put(K key, V value) throws Exception {
        cache.put(key, value);
    }

    @Override
    public V get(K key) throws Exception {
        return cache.get(key);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public CacheInfo getCacheInfo() {
        CacheInfo info = new CacheInfo();
        info.setCurrentSizeC1(cache.size());
        return info;
    }

    @Override
    public Collection<V> values() throws Exception {
        return cache.values();
    }

    @Override
    public Set<K> keys() throws Exception {
        return cache.keySet();
    }

    @Override
    public Iterator<K> iterator() throws Exception {
        return cache.keySet().iterator();
    }

    @Override
    public Iterator<V> iterator2() throws Exception {
        return cache.values().iterator();
    }

    @Override
    public void removeAll(Set keys) throws Exception {
        for (Object key : keys) {
			cache.remove(key);
		}
    }

    @Override
    public boolean containsKey(K key) {
        return  cache.containsKey(key);
    }

    @Override
    public V remove(K key) throws Exception {
        return cache.remove(key);
    }

    @Override
    public void put(Map<K, V> entries) throws Exception {
        cache.putAll(entries);

    }

    @Override
    public K getKey(Object entry) {
        
        return null;
    }

    @Override
    public V getValue(Object entry) {
        // TODO Auto-generated method stub
        if(cache.containsKey(entry)) return (V) entry;
        return null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean isAccessable() {
        return true;
    }

}
