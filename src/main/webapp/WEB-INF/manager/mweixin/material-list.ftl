<!--图文素材页-->

<!DOCTYPE html>
<html>
   <head>
      <meta charset="UTF-8">
      <title></title>
      <!-- <#include "/include/head-file.ftl"/> -->
      <!--#include virtual="../include/head-file.ftl" -->
      <link rel="stylesheet" href="../../../static/mweixin/css/material-list.css">
   </head>
   <body>
      <div id="app">
         <el-container>
            <el-aside width="140px">
               <!--#include virtual="../include/menu.ftl" -->
            </el-aside>
            <el-container>
               <el-header>
                  <!--#include virtual="../include/head.ftl" -->
               </el-header>
               <el-main>
                  <!--#include virtual="../include/head.ftl" -->
               </el-main>
            </el-container>
         </el-container>
      </div>
   </body>
</html>

<script>
   new Vue({
      el: "#app",
      data: {},
      methods: {},
      mounted: function() {}
   })
</script>