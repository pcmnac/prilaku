package com.github.pcmnac.prilaku;

public class BaseBehavior<D> implements IBehavior<D>
{
    private D domain;

    @Override
    public void set(D domain)
    {
        this.domain = domain;
    }

    @Override
    public D get()
    {
        return domain;
    }

}
