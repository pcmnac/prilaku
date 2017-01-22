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
import java.util.ServiceLoader;
import java.util.Set;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import com.github.pcmnac.prilaku.annotation.Behavior;
import com.github.pcmnac.prilaku.annotation.BehaviorOf;
import com.github.pcmnac.prilaku.annotation.Domain;
import com.github.pcmnac.prilaku.provider.DefaultInstanceProvider;
import com.github.pcmnac.prilaku.provider.InstanceProvider;
import com.google.common.base.Predicate;

/**
 * @author pcmnac@gmail.com
 * 
 */
@SuppressWarnings("unchecked")
public class Pku
{

    public static class Enhanced
    {
        private Object domainObject;

        public Enhanced(Object domainObject)
        {
            this.domainObject = domainObject;
        }

        public <T> T get(Class<? extends T> behaviorInterfaceType)
        {
            return Pku.get(domainObject, behaviorInterfaceType);
        }

    }

    public static Enhanced $(Object domainObject)
    {
        return new Enhanced(domainObject);
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

    private static InstanceProvider provider;

    private static final Predicate<Field> DOMAIN_FIELD_PREDICATE = new Predicate<Field>()
    {
        @Override
        public boolean apply(Field input)
        {
            return input.isAnnotationPresent(Domain.class);
        }
    };

    private static final Predicate<Class<?>> BEHAVIOR_PREDICATE = new Predicate<Class<?>>()
    {
        @Override
        public boolean apply(Class<?> type)
        {
            return type.isAnnotationPresent(Behavior.class);
        }
    };

    public static <T> T get(Object domainObject, Class<? extends T> behaviorInterfaceType)
    {

        // Gets appropriate behavior implementation from local cache...
        Object behaviorImpl = cache.get(new Pair(behaviorInterfaceType, domainObject.getClass()));
        Class<?> domainType = domainObject.getClass();

        // If theres no entry for required behavior on cache...
        if (behaviorImpl == null)
        {
            // Looks for the behavior implementation...
            try
            {
                Map<Class<?>, Class<?>> implementations = map.get(behaviorInterfaceType);

                if (implementations == null)
                {
                    throw new RuntimeException(
                            String.format("No implementations found for behavior: %s", behaviorInterfaceType));
                }

                // Scans domain class and its super classes looking for an behavior implementation of required
                // interface.
                Class<?> treeDomainClass = domainType;
                do
                {
                    Class<?> behaviorClass = implementations.get(treeDomainClass);

                    // if is there a valid behavior implementation...
                    if (behaviorClass != null)
                    {
                        // Gets the behavior implementation by using a instance provider.

                        // if there is no provider set...
                        if (provider == null)
                        {
                            // Loads custom provider...
                            ServiceLoader<InstanceProvider> providers = ServiceLoader.load(InstanceProvider.class);
                            // if there is a custom instance provider...
                            if (providers.iterator().hasNext())
                            {
                                // uses it.
                                provider = providers.iterator().next();
                            }
                            // otherwise...
                            else
                            {
                                // uses the default one.
                                provider = new DefaultInstanceProvider();
                            }
                        }

                        // gets the instance.
                        behaviorImpl = provider.get(behaviorClass);

                        // Injects the domain object on annotated field(s).
                        Set<Field> fields = ReflectionUtils.getAllFields(behaviorClass, DOMAIN_FIELD_PREDICATE);

                        for (Field field : fields)
                        {
                            field.setAccessible(true);
                            field.set(behaviorImpl, domainObject);
                        }
                    }
                    // if is there no valid behavior implementation...
                    else
                    {
                        // Looks at its super class...
                        treeDomainClass = treeDomainClass.getSuperclass();
                    }
                }
                // repeats the process until find a valid behavior implementation or reaches Object class on the
                // hierarchy.
                while (!treeDomainClass.equals(Object.class) && behaviorImpl == null);

                // If there's no behavior implementation for requested domain object and interface...
                if (behaviorImpl == null)
                {
                    // Throws an exception.
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

        System.out.println(String.format("Registering behavior implementation (%s) for domain (%s) and behavior (%s).",
                behaviorImplementationType, domainClass, behaviorInterfaceType));

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

            Set<Class<?>> behaviors = ReflectionUtils.getAllSuperTypes(behaviorImplType, BEHAVIOR_PREDICATE);

            for (Class<?> behaviorType : behaviors)
            {
                System.out.println(
                        String.format("Behavior implementation detected: (%s) for domain (%s) and behavior (%s).",
                                behaviorImplType, domain.value(), behaviorType));
                register(domain.value(), behaviorType, behaviorImplType);
            }

        }
    }

}
