.class public Lcom/oscar0812/sample_navigation/StringUtil;
.super Ljava/lang/Object;
.source "StringUtil.java"


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static a()Ljava/lang/String;
    .locals 1

    new-instance v0, Lcom/oscar0812/sample_navigation/StringUtil$1;

    invoke-direct {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;-><init>()V

    invoke-virtual {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static b()Ljava/lang/String;
    .locals 1

    new-instance v0, Lcom/oscar0812/sample_navigation/StringUtil$2;

    invoke-direct {v0}, Lcom/oscar0812/sample_navigation/StringUtil$2;-><init>()V

    invoke-virtual {v0}, Lcom/oscar0812/sample_navigation/StringUtil$2;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method
