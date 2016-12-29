package com.github.pcmnac.prilaku;

public interface IBehavior<D>
{
    void set(D domain);
    
    D get();
}
