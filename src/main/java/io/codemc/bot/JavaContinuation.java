package io.codemc.bot;

import kotlin.Result;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class JavaContinuation<T> implements Continuation<T> {
    public static final JavaContinuation<Unit> UNIT = new JavaContinuation<>(CompletableFuture.completedFuture(Unit.INSTANCE));

    public static <T> JavaContinuation<T> create(CompletableFuture<T> future) {
        return new JavaContinuation<>(future);
    }

    private final CompletableFuture<T> future;

    public JavaContinuation(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void resumeWith(@NotNull Object o) {
        if (o instanceof Result.Failure)
            future.completeExceptionally(((Result.Failure) o).exception);
        else
            future.complete((T) o);
    }


    @NotNull
    @Override
    public CoroutineContext getContext() {
        return Dispatchers.getIO();
    }
}
