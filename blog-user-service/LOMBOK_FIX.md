# 修复 blog-user-service 的 Lombok 问题的说明文档

## 问题诊断

Maven 编译时 Lombok 注解处理器没有正常工作，导致所有的 `@Data` 注解生成的 getter/setter 方法找不到。

## 解决方案

为了与项目中其他服务（blog-article-service）保持一致，我们将：

1. **移除 Lombok 依赖**：从 pom.xml 中删除 Lombok
2. **手动添加 Getter/Setter**：为所有实体类和 DTO 手写 getter/setter 方法

这种方式虽然代码较多，但更加可靠，不依赖注解处理器。

## 需要修改的文件

1. `pom.xml` - 移除 Lombok 依赖
2. `User.java` - 添加 getter/setter
3. `UserAuth.java` - 添加 getter/setter
4. `RegistrationRequest.java` - 添加 getter/setter
5. `UserDTO.java` - 添加 getter/setter
6. `UserAuthDetailsDTO.java` - 添加 getter/setter（已有构造函数，需补充 getter/setter）

## 执行步骤

运行以下命令修复问题：

```powershell
# 1. 清理之前的构建
cd c:\Users\lenovo\Desktop\blog\blog-user-service
mvn clean

# 2. 修复所有Java文件（手动或使用IDE生成getter/setter）

# 3. 重新编译
mvn clean compile

# 4. 打包
mvn clean package -DskipTests

# 5. 运行
java -jar target/blog-user-service-0.0.1-SNAPSHOT.jar
```

## 预期结果

修复后，服务应该能够成功编译并启动在端口 8083。
