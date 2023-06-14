package com.atguigu.gmall.product.controller;
/**
 * @ClassName: FileUploadController
 * @author: javaermamba
 * @date: 2023-06-2023/6/14-18:33
 * @Description: 文件上传控制器
 */
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/product")
public class FileUploadController {

    //  获取到文件服务器URL地址！ 软编码！ 硬编码写在Java 文件中！
    //  @Value 注解：使用的时候，前提条件是这个类 必须被spring 容器管理！
    @Value("${minio.endpointUrl}")
    private String endpointUrl; // endpointUrl=http://192.168.200.129:9000

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secreKey;

    @Value("${minio.bucketName}")
    public String bucketName;


    //  /admin/product/fileUpload
    //  springmvc : 文件上传！ file 对象名称 必须要与vue 传递的name 属性值一样！
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) {
        //  声明一个url 变量！
        String url = "";

        try {
            //  先获取minioClient 的客户端
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();

            //  创建存储桶！
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket 'asiatrip' already exists.");
            }
            //  文档！
            //                minioClient.uploadObject(
            //                        UploadObjectArgs.builder()
            //                                .bucket("asiatrip")
            //                                .object("asiaphotos-2015.zip")
            //                                .filename("/home/user/Photos/asiaphotos.zip")
            //                                .build());
            //  获取文件的后缀名
            String originalFilename = file.getOriginalFilename(); // atguigu.jpg;
            //  获取文件名称
            String fileName = UUID.randomUUID().toString() + "." + StringUtils.getFilenameExtension(originalFilename);
            //  String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));
            //  fileName = atguigu123afakajsdf.jpg | png
            System.out.println("file:"+file);
            // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
            //        minioClient.uploadObject(
            //                UploadObjectArgs.builder()
            //                        .bucket(bucketName)
            //                        .object(fileName)
            //                        .filename("/home/user/Photos/asiaphotos.zip")
            //                        .build());
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //  url: http://192.168.200.129:9000/gmall/67204606-8538-4413-b62d-138d813fabc4.jpg
            url=endpointUrl+"/"+bucketName+"/"+fileName;
            System.out.println("url:\t"+url);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }
        return Result.ok(url);
    }

}
