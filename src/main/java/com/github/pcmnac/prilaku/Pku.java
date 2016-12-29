/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 *                                               Histórico de Revisões
 * --------------------------------------------------------------------------------------------------------------------
 * Autor                                      Data       ID Tarefa                       Comentários
 * --------------------------------------|------------|--------------|-------------------------------------------------
 * 04343650413 (pcc)                       06/09/2016                  Versão inicial.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.github.pcmnac.prilaku;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import com.github.pcmnac.prilaku.annotation.Behavior;
import com.github.pcmnac.prilaku.annotation.BehaviorOf;
import com.github.pcmnac.prilaku.annotation.DomainInstance;
import com.google.common.base.Predicate;

/**
 * @author pcmnac@gmail.com
 * 
 */
@SuppressWarnings("unchecked")
public class Pku
{

    public static class Holder
    {
        private Object domainObject;

        public Holder(Object domainObject)
        {
            this.domainObject = domainObject;
        }

        public <T> T get(Class<? extends T> behaviorInterfaceType)
        {
            return Pku.get(domainObject, behaviorInterfaceType);
        }

    }

    public static Holder $(Object domainObject)
    {
        return new Holder(domainObject);
    }

    private static class Pair
    {
        Class<?> behavior;
        Class<?> domainClass;

        /**
         * @param behavior
         * @param domainClass
         */
        public Pair(Class<?> behavior, Class<?> domainClass)
        {
            super();
            this.behavior = behavior;
            this.domainClass = domainClass;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((behavior == null) ? 0 : behavior.hashCode());
            result = prime * result + ((domainClass == null) ? 0 : domainClass.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Pair other = (Pair) obj;
            if (behavior == null)
            {
                if (other.behavior != null)
                    return false;
            }
            else if (!behavior.equals(other.behavior))
                return false;
            if (domainClass == null)
            {
                if (other.domainClass != null)
                    return false;
            }
            else if (!domainClass.equals(other.domainClass))
                return false;
            return true;
        }

    }

    private static Map<Class<?>, Map<Class<?>, Class<?>>> map = new HashMap<Class<?>, Map<Class<?>, Class<?>>>();

    private static Map<Pair, Object> cache = new HashMap<Pair, Object>();

    public static <T> T get(Object domainObject, Class<? extends T> behaviorInterfaceType)
    {

        Object behaviorImpl = cache.get(new Pair(behaviorInterfaceType, domainObject.getClass()));
        Class<?> domainType = domainObject.getClass();

        if (behaviorImpl == null)
        {
            try
            {
                Map<Class<?>, Class<?>> implementations = map.get(behaviorInterfaceType);

                if (implementations == null)
                {
                    throw new RuntimeException(
                            String.format("No implementations found for behavior: %s", behaviorInterfaceType));
                }

                Class<?> treeDomainClass = domainType;
                do
                {
                    Class<?> behaviorClass = implementations.get(treeDomainClass);
                    if (behaviorClass == null)
                    {
                        treeDomainClass = treeDomainClass.getSuperclass();
                    }
                    else
                    {
                        behaviorImpl = behaviorClass.newInstance();

                        Set<Field> fields = ReflectionUtils.getAllFields(behaviorClass, new Predicate<Field>()
                        {
                            @Override
                            public boolean apply(Field input)
                            {
                                return input.isAnnotationPresent(DomainInstance.class);
                            }
                        });

                        for (Field field : fields)
                        {
                            field.setAccessible(true);
                            field.set(behaviorImpl, domainObject);
                        }

                        // cache.put(new Pair(behaviorInterfaceType, domainType), behaviorImpl);
                    }
                }
                while (!treeDomainClass.equals(Object.class) && behaviorImpl == null);

                if (behaviorImpl == null)
                {
                    throw new RuntimeException(String.format("No implementation found for domain: %s and behavior: %s",
                            domainType, behaviorInterfaceType));
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException("Error creating behavior instance", e);
            }
        }

        return (T) behaviorImpl;

    }

    public static void register(Class<?> domainClass, Class<?> behaviorInterfaceType,
            Class<?> behaviorImplementationType)
    {

        Map<Class<?>, Class<?>> implementations = map.get(behaviorInterfaceType);

        if (implementations == null)
        {
            implementations = new HashMap<Class<?>, Class<?>>();
            map.put(behaviorInterfaceType, implementations);
        }

        implementations.put(domainClass, behaviorImplementationType);

    }

    public static void registerAnnotated(String packageName)
    {
        Reflections reflections = new Reflections(packageName);

        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BehaviorOf.class);

        for (Class<?> behaviorImplType : annotated)
        {

            BehaviorOf domain = behaviorImplType.getAnnotation(BehaviorOf.class);

            Set<Class<?>> behaviors = ReflectionUtils.getAllSuperTypes(behaviorImplType, new Predicate<Class<?>>()
            {
                @Override
                public boolean apply(Class<?> type)
                {
                    return type.isAnnotationPresent(Behavior.class);
                }
            });

            for (Class<?> behaviorType : behaviors)
            {
                System.out.println(
                        String.format("Registering behavior implementation (%s) for domain (%s) and behavior(%s).",
                                behaviorImplType, domain.value(), behaviorType));
                register(domain.value(), behaviorType, behaviorImplType);
            }

        }
    }

}
