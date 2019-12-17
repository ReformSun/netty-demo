package cn.morethink.netty.router;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Data
@RequiredArgsConstructor
@Slf4j
public class Action<T> {
    @NonNull
    private Object object;
    @NonNull
    private Method method;

    private boolean injectionFullhttprequest;

    public T call(Object... args) {
        try {
            return (T) method.invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
//            log.warn("{}", e);
        }
        return null;
    }

    public boolean isInjectionFullhttprequest(){
        return injectionFullhttprequest;
    }


}