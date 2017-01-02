package com.github.pcmnac.prilaku.provider;

public class DefaultInstanceProvider implements InstanceProvider
{

    @Override
    public Object get(Class<?> type) throws Exception
    {
        return type.newInstance();
    }

}
