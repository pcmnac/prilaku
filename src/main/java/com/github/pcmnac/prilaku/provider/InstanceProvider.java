package com.github.pcmnac.prilaku.provider;

public interface InstanceProvider
{
    Object get(Class<?> type) throws Exception;
}
