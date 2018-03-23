package com.gateway.controller;

import java.util.HashMap;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.common.base.BaseController;
import com.common.base.BaseResponse;
import com.common.model.User;
import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
@RestController
@Api(tags = { "登录" })
@RequestMapping(value = "/gpaas")
public class LoginController extends BaseController {
	
	@Value("${security.oauth2.client.clientId}")
	private String oAuth2ClientId;

	@Value("${security.oauth2.client.clientSecret}")
	private String oAuth2ClientSecret;

	@Value("${security.oauth2.client.accessTokenUri}")
	private String accessTokenUri;
	
	@Value("${hiacloud_io_ipAddress}")
	private String hiacloud_io_ipAddress;
	
	@ApiOperation(value = "登录", notes = "登录", produces = "application/json")
	@RequestMapping(value = "/goLogin", method = RequestMethod.POST)
	public BaseResponse<?> goLogin(@ApiParam("{\"username\":\"xxx\",\"password\":\"xxx\"}") @RequestBody User user) {
		try {
			HttpHeaders headers=new HttpHeaders();
			headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			HttpEntity<String> entity=new HttpEntity<String>("client_id="+oAuth2ClientId+"&client_secret="+oAuth2ClientSecret+"&username="+user.getUsername()+"&password="+user.getPassword()+"&grant_type=password",headers);
			RestTemplate restTemplate = new RestTemplate();
			// 访问UAA获取Token
			ResponseEntity<String> result = restTemplate.exchange(accessTokenUri,HttpMethod.POST,entity,String.class);
			
			// 使用token从cloud服务中获取用户信息
			if(result.getStatusCodeValue()==200){
				String body = result.getBody();
				Gson gson = new Gson();
				HashMap map =  gson.fromJson(body, HashMap.class);
				headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
				headers.set("Authorization", "bearer "+(String) map.get("access_token"));
				ResponseEntity<String> response = restTemplate.exchange(hiacloud_io_ipAddress+"/cloud/user/queryUser",HttpMethod.GET,new HttpEntity<String>(headers),String.class);
				String body2 = response.getBody();
				HashMap map2 = gson.fromJson(body2, HashMap.class);
				map.put("current_user", map2.get("obj"));
				return BaseResponse.getResponse(true, "登录成功", map);
			}
			return BaseResponse.getResponse(false, "登录失败,服务器内部错误", null);

		} catch (Exception e) {
			return BaseResponse.getResponse(false, "账号或密码错误", null);
			// TODO: handle exception
		}

	}
}
