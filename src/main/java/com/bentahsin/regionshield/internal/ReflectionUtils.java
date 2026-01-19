package com.bentahsin.regionshield.internal;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Java Reflection işlemlerini güvenli ve temiz bir şekilde yapmak için yardımcı araçlar.
 * try-catch bloklarını tek bir yerde toplar ve kod kirliliğini önler.
 */
@UtilityClass
public class ReflectionUtils {

    /**
     * İsmi verilen sınıfı bulmaya çalışır.
     * @param className Sınıfın tam paket adı (örn: com.sk89q.worldguard.WorldGuard)
     * @return Sınıf bulunursa Class objesi, bulunamazsa null.
     */
    public Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Bir sınıftaki metodu ismine ve parametrelerine göre bulur.
     * @param clazz Aranacak sınıf
     * @param methodName Metod adı
     * @param parameterTypes Metodun parametre tipleri
     * @return Metod bulunursa Method objesi, bulunamazsa null.
     */
    public Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) return null;
        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Bir metodu güvenli bir şekilde çalıştırır.
     * @param method Çalıştırılacak metod
     * @param instance Hangi obje üzerinde çalışacak? (Static metodlar için null olabilir)
     * @param args Metoda gönderilecek parametreler
     * @return Metodun dönüş değeri (Object) veya hata durumunda null.
     */
    public Object invoke(Method method, Object instance, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(instance, args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Bir sınıfın içindeki alanın (field/değişken) değerini okur.
     * @param clazz Sınıf
     * @param instance Obje örneği
     * @param fieldName Değişken adı
     * @return Değişkenin değeri
     */
    public Object getField(Class<?> clazz, Object instance, String fieldName) {
        if (clazz == null) return null;
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Bir eklentinin sunucuda yüklü ve aktif olup olmadığını kontrol eder.
     * @param pluginName Eklenti adı (plugin.yml içindeki name)
     */
    public boolean isPluginActive(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}