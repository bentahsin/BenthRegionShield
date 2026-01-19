package com.bentahsin.regionshield.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Java Reflection (Yansıtma) işlemlerini basitleştirmek ve güvenli hale getirmek için bir dizi statik yardımcı metot sağlar.
 * <p>
 * Bu sınıfın temel amacı, yansıtma ile ilişkili istisna yönetimini (try-catch blokları) merkezileştirerek
 * kod tekrarını önlemek ve proje genelinde okunabilirliği artırmaktır. Tüm metotlar,
 * bir hata durumunda istisna fırlatmak yerine null döndürür, bu da kullanımı kolaylaştırır.
 * <p>
 * Bir {@link UtilityClass} olarak bu sınıfın bir örneği oluşturulamaz.
 */
@UtilityClass
@SuppressFBWarnings("REFLF_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_FIELD")
public class ReflectionUtils {

    /**
     * Tam nitelikli adına göre bir sınıfı güvenli bir şekilde bulmaya ve yüklemeye çalışır.
     * Bu metot, {@link ClassNotFoundException} istisnasını kapsüller.
     *
     * @param className Sınıfın tam paket adı (örneğin: "com.sk89q.worldguard.WorldGuard").
     * @return Sınıf bulunursa {@link Class} nesnesi, bulunamazsa null.
     */
    public Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Verilen bir sınıfta, adına ve parametre türlerine göre bir metot bulur.
     * Özel veya korumalı metotlara erişime izin vermek için bulunan metot üzerinde
     * otomatik olarak {@code setAccessible(true)} çağrısı yapar.
     *
     * @param clazz          Metodun aranacağı sınıf.
     * @param methodName     Aranacak metodun adı.
     * @param parameterTypes Metodun sahip olduğu parametrelerin türleri.
     * @return Metot bulunursa {@link Method} nesnesi, bulunamazsa null.
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
     * Belirtilen argümanlarla, verilen bir örnek üzerinde bir metodu güvenli bir şekilde çağırır.
     * Metot çağrımı sırasında fırlatılan herhangi bir istisna yakalanır ve metot null döndürür.
     *
     * @param method   Çalıştırılacak olan {@link Method} nesnesi.
     * @param instance Metodun çağrılacağı nesne örneği. Statik metotlar için bu değer null olabilir.
     * @param args     Metoda geçirilecek olan argümanlar (parametreler).
     * @return Metodun dönüş değeri veya bir hata oluşursa null. Eğer metodun dönüş tipi void ise null döner.
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
     * Verilen bir örnekten bir alanın (field) değerini güvenli bir şekilde okur.
     * Bu metot, {@code getDeclaredField} kullanarak ve erişilebilir olarak ayarlayarak özel alanlara erişebilir.
     *
     * @param clazz     Alanın bulunduğu sınıf.
     * @param instance  Değerin okunacağı nesne örneği.
     * @param fieldName Değeri okunacak olan alanın adı.
     * @return Alanın değeri veya bir hata oluşursa (örn: alan bulunamadı) null.
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
     *
     * @param pluginName Eklentinin adı (plugin.yml dosyasındaki 'name' değeri).
     * @return Eklenti aktif ise true, değilse false.
     */
    public boolean isPluginActive(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}