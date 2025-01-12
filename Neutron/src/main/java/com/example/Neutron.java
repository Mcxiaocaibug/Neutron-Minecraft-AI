package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Neutron extends JavaPlugin {

    private String geminiApiEndpoint; // 从配置文件读取API地址
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();
    private String apiKey = "your-api-key";//此处apikey 实际上用不到，兼容OpenAI格式

    @Override
    public void onEnable() {
        saveDefaultConfig(); // 保存默认配置到config.yml
        geminiApiEndpoint = getConfig().getString("gemini_api_endpoint"); // 从配置文件读取
        if (geminiApiEndpoint == null || geminiApiEndpoint.isEmpty()) {
            getLogger().warning("gemini_api_endpoint not found in config.yml, please check configuration");
        }
        getLogger().info("Neutron Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Neutron Plugin disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("ai")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage("请提供你的问题,例如:/ai 你好");
                return true;
            }
            // 将args转化为String
            StringBuilder input = new StringBuilder();
            for (String word : args) {
                input.append(word).append(" ");
            }

            String prompt = input.toString().trim();//去除空格
            sendMessage(prompt, player);//发送消息

            return true;
        }
        return false;
    }

     public void sendMessage(String prompt, Player player) {

       CompletableFuture.supplyAsync(() -> {
         try {
             MediaType mediaType = MediaType.parse("application/json");
              //构造请求参数 模拟 OpenAI 的请求参数
             JsonObject requestBody = new JsonObject();
             requestBody.addProperty("model", "gemini-pro");
             JsonArray messages = new JsonArray();
             JsonObject message = new JsonObject();
             message.addProperty("role", "user");
             message.addProperty("content", prompt);
             messages.add(message);
              requestBody.add("messages",messages);

             RequestBody body = RequestBody.create(requestBody.toString(),mediaType);

            Request request = new Request.Builder()
                    .url(geminiApiEndpoint)
                     .addHeader("Authorization","Bearer "+apiKey)//兼容OpenAI格式，无需做鉴权。 你可以添加自己Gemini的鉴权机制
                    .post(body)
                    .build();


                Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
              if(response.body() == null)
              {
                 player.sendMessage("The returned result is null，Please contact the administrator");
                return null;
              }
               String responseBody = response.body().string();
                // 解析响应内容
              JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

              if(jsonResponse.getAsJsonArray("choices") == null || jsonResponse.getAsJsonArray("choices").size() <= 0 )
              {
                   player.sendMessage("The returned result choices field is not correct , Please contact the administrator");
                   return null;
              }
                String reply = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                   .getAsJsonObject("message").get("content").getAsString();

                  return reply;

           }else {
                String errBody  = (response.body() !=null ) ?  response.body().string() : "";
               getLogger().warning("Send Message error ,response code "+response.code()+ " ,response body "+errBody);
               player.sendMessage("An error occurred while processing the request,please check the server log for details ");
            }
         }catch (IOException e){
             getLogger().warning("Send Message error,  exception " + e.getMessage());
           player.sendMessage("An error occurred while processing the request,please check the server log for details ");

         }

         return  null;
        }).thenAccept( reply -> {
            if(reply != null && !reply.isEmpty())
            {
                player.sendMessage(reply);
            }
            else{
             //Do Nothing  因为错误信息在执行异步的时候发送给玩家了
            }
         });
   }

}