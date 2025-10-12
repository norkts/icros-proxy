# icros-proxy
通过请求url前缀替换, 代理http请求解决浏览器跨域问题

# build
编译打包执行
``
./package.sh
``

# run
``
java -jar output/proxy-web-1.0-SNAPSHOT.jar
``

# proxy
对于任意需要代理url, 直接添加前缀http://127.0.0.1:8080/proxy/, 
其他请求参数不需要做任何修改
``
curl -i http://127.0.0.1:8080/proxy/https://www.baidu.com
``
支持https和http协议
支持GET,POST,OPTIONS请求
