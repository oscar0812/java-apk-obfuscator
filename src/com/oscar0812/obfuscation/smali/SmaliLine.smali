.class public Lcom/oscar0812/obfuscation/smali/SmaliLine;
.super Ljava/lang/Object;
.source "SmaliLine.java"


# instance fields
.field public parentClass:Lcom/oscar0812/obfuscation/smali/SmaliClass;

.field private final parts:[Ljava/lang/String;

.field private final text:Ljava/lang/String;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .registers 4
    .param p1, "text"    # Ljava/lang/String;

    .prologue
    .line 21
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 22
    iput-object p1, p0, Lcom/oscar0812/obfuscation/smali/SmaliLine;->text:Ljava/lang/String;

    .line 23
    invoke-virtual {p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    const-string v1, "\\s+"

    invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/oscar0812/obfuscation/smali/SmaliLine;->parts:[Ljava/lang/String;

    .line 24
    return-void
.end method

.method private static ignoreLine(Ljava/lang/String;)Z
    .registers 4
    .param p0, "text"    # Ljava/lang/String;

    .prologue
    .line 39
    invoke-virtual {p0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    .line 40
    .local v1, "trimmed":Ljava/lang/String;
    const-string v2, ".line"

    invoke-virtual {v1, v2}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    .line 42
    .local v0, "ignore":Z
    return v0
.end method

.method public static process(Ljava/lang/String;)Ljava/util/ArrayList;
    .registers 6
    .param p0, "text"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            ")",
            "Ljava/util/ArrayList",
            "<",
            "Lcom/oscar0812/obfuscation/smali/SmaliLine;",
            ">;"
        }
    .end annotation

    .prologue
    .line 47
    new-instance v0, Lcom/oscar0812/obfuscation/smali/SmaliLine;

    invoke-direct {v0, p0}, Lcom/oscar0812/obfuscation/smali/SmaliLine;-><init>(Ljava/lang/String;)V

    .line 48
    .local v0, "originalLine":Lcom/oscar0812/obfuscation/smali/SmaliLine;
    new-instance v2, Ljava/util/ArrayList;

    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    .line 50
    .local v2, "smaliLines":Ljava/util/ArrayList;, "Ljava/util/ArrayList<Lcom/oscar0812/obfuscation/smali/SmaliLine;>;"
    invoke-static {p0}, Lcom/oscar0812/obfuscation/smali/SmaliLine;->ignoreLine(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_2f

    .line 51
    invoke-virtual {v0}, Lcom/oscar0812/obfuscation/smali/SmaliLine;->getParts()[Ljava/lang/String;

    move-result-object v1

    .line 52
    .local v1, "parts":[Ljava/lang/String;
    invoke-virtual {v0}, Lcom/oscar0812/obfuscation/smali/SmaliLine;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_2f

    const/4 v3, 0x0

    aget-object v3, v1, v3

    const-string v4, "const-string\""

    invoke-virtual {v3, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_2f

    .line 55
    sget-object v3, Ljava/lang/System;->out:Ljava/io/PrintStream;

    array-length v4, v1

    add-int/lit8 v4, v4, -0x1

    aget-object v4, v1, v4

    invoke-virtual {v3, v4}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 60
    .end local v1    # "parts":[Ljava/lang/String;
    :cond_2f
    return-object v2
.end method


# virtual methods
.method public getParts()[Ljava/lang/String;
    .registers 2

    .prologue
    .line 31
    iget-object v0, p0, Lcom/oscar0812/obfuscation/smali/SmaliLine;->parts:[Ljava/lang/String;

    return-object v0
.end method

.method public getText()Ljava/lang/String;
    .registers 2

    .prologue
    .line 27
    iget-object v0, p0, Lcom/oscar0812/obfuscation/smali/SmaliLine;->text:Ljava/lang/String;

    return-object v0
.end method

.method public isEmpty()Z
    .registers 2

    .prologue
    .line 35
    iget-object v0, p0, Lcom/oscar0812/obfuscation/smali/SmaliLine;->text:Ljava/lang/String;

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v0

    return v0
.end method
