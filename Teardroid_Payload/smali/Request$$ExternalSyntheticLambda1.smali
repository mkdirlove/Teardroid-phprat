.class public final synthetic LRequest$$ExternalSyntheticLambda1;
.super Ljava/lang/Object;

# interfaces
.implements Lcom/android/volley/Response$ErrorListener;


# instance fields
.field public final synthetic f$0:LRequest;


# direct methods
.method public synthetic constructor <init>(LRequest;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, LRequest$$ExternalSyntheticLambda1;->f$0:LRequest;

    return-void
.end method


# virtual methods
.method public final onErrorResponse(Lcom/android/volley/VolleyError;)V
    .locals 1

    iget-object v0, p0, LRequest$$ExternalSyntheticLambda1;->f$0:LRequest;

    invoke-static {v0, p1}, LRequest;->$r8$lambda$lAKJTI7yaSOi_4lwx5DEDYpjTus(LRequest;Lcom/android/volley/VolleyError;)V

    return-void
.end method
