package com.bizzan.er.normal.job;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.bizzan.er.normal.robot.ExchangeRobotCustom;
import com.bizzan.er.normal.service.CustomRobotKlineService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import com.bizzan.er.normal.entity.RobotParams;
import com.bizzan.er.normal.robot.ExchangeRobot;
import com.bizzan.er.normal.robot.ExchangeRobotFactory;
import com.bizzan.er.normal.robot.ExchangeRobotNormal;
import com.bizzan.er.normal.service.RobotParamService;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class RobotJob {
	private final static  Logger logger  =  LoggerFactory.getLogger(RobotJob.class);
	@Autowired
	private ExchangeRobotFactory exchangeRobotFactory;
	
	@Autowired
    private RestTemplate restTemplate;
	
	@Autowired
	private RobotParamService robotParamService;

	@Autowired
	private CustomRobotKlineService customRobotKlineService;
	
	@Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;
    @Value("${spark.system.host}")
    private String host;
    @Value("${spark.system.name}")
    private String company;

    @Value("${spark.system.admins}")
    private String admins;
    
    @Value("${spark.system.admin-phones}")
    private String adminPhones;
	
	private boolean inited = false;
	
	@Scheduled(fixedDelay = 20000)
	public void synchronizeExchangeCenter(){
		String serviceName = "SERVICE-EXCHANGE-TRADE";
        String url = "http://" + serviceName + "/monitor/engines";
        ResponseEntity<HashMap> resultStr = restTemplate.getForEntity(url, HashMap.class);
        Map<String, Integer> exchangeCenterCoins = (HashMap<String, Integer>)resultStr.getBody();
        for (Map.Entry<String, Integer> coin : exchangeCenterCoins.entrySet()) {
        	String coinName = coin.getKey();
        	if(!exchangeRobotFactory.containsExchangeRobot(coinName)) {
	        	RobotParams params = robotParamService.findOne(coinName);
	        	if(params != null) {
	        		ExchangeRobot robot = null;
	        		if(params.getRobotType() == 0) {
	        			logger.info("????????????????????????" + coinName);
	        			robot = new ExchangeRobotNormal();
					}else if(params.getRobotType() == 1){
						logger.info("????????????????????????" + coinName);
						robot = new ExchangeRobotCustom();
					}
					robot.setRobotParamSevice(robotParamService);
	        		robot.setCustomRobotKlineService(customRobotKlineService);
					robot.setRestTemplate(restTemplate);

					robot.setRobotParams(params);
					robot.setPlateSymbol(coinName);

					exchangeRobotFactory.addExchangeRobot(params.getCoinName(), robot);
					if(params.getRobotType() == 0) {
						new Thread((ExchangeRobotNormal) robot).start();
					}else{
						new Thread((ExchangeRobotCustom) robot).start();
					}
	        	}
        	}
        }
	}
	
	@Scheduled(fixedDelay = 120000)
	public void checkAllRoot(){
		List <String> runRobotList = new ArrayList<String>();
		List <String> haltRobotList = new ArrayList<String>();
		List <String> errorRobotList = new ArrayList<String>();
		
		Instant currentT = Instant.now();
		logger.info("??????????????????" + exchangeRobotFactory.getRobotList().size());
		exchangeRobotFactory.getRobotList().forEach((symbol, robot)->{
			if(robot.getRobotParams().isHalt()) {
				haltRobotList.add("[" + symbol + "]");
			}else {
				Instant timestamp = Instant.ofEpochMilli(robot.getLastSendOrderTime().toEpochMilli());
			    ZonedDateTime losAngelesTime = timestamp.atZone(ZoneId.of("Asia/Shanghai"));
				runRobotList.add("[" + symbol + "] - ???????????????" + losAngelesTime.toLocalDateTime());
				// ???????????????????????????????????????
				if(currentT.toEpochMilli() - timestamp.toEpochMilli() > robot.getRobotParams().getRunTime() * 5) {
					errorRobotList.add(symbol);
				}
			}
		});
		
		logger.info("?????????????????????: " + runRobotList.size());
		for(int i = 0; i < runRobotList.size(); i++) {
			logger.info(runRobotList.get(i));
		}
		
		logger.info("?????????????????????: " + haltRobotList.size());
		for(int i = 0; i < haltRobotList.size(); i++) {
			logger.info(haltRobotList.get(i));
		}
		
		logger.info("????????????????????????: " + errorRobotList.size());
		for(int i = 0; i < errorRobotList.size(); i++) {
			logger.info(errorRobotList.get(i));
			String coinName = errorRobotList.get(i);

			// ???????????????????????????????????????????????????
			ExchangeRobot deadRobot = exchangeRobotFactory.getExchangeRobot(coinName);
			if(deadRobot != null) {
                if(deadRobot.isRunning()) {
                    deadRobot.interrupt();
                }
                // ???????????????
                if(deadRobot.getRobotParams().getRobotType() == 0) {
                    logger.info("?????????????????????");
                    new Thread((ExchangeRobotNormal) deadRobot).start();
                }else{
                    logger.info("?????????????????????");
                    new Thread((ExchangeRobotCustom) deadRobot).start();
                }
			}else{
				logger.info("?????????????????????: " + coinName);
				logger.info("???????????????: " + coinName);
				// ??????????????????????????????????????????????????????
				RobotParams params = robotParamService.findOne(coinName);
				if(params != null) {
					logger.info("????????????: " + coinName);
					ExchangeRobot robot = null;
					if(params.getRobotType() == 0) {
						logger.info("????????????????????????" + coinName);
						robot = new ExchangeRobotNormal();
					}else if(params.getRobotType() == 1){
						logger.info("????????????????????????" + coinName);
						robot = new ExchangeRobotCustom();
					}
					robot.setRobotParamSevice(robotParamService);
					robot.setCustomRobotKlineService(customRobotKlineService);
					robot.setRestTemplate(restTemplate);

					robot.setRobotParams(params);
					robot.setPlateSymbol(coinName);

					exchangeRobotFactory.forceAddExchangeRobot(params.getCoinName(), robot);
					if(params.getRobotType() == 0) {
						logger.info("?????????????????????: " + coinName);
						new Thread((ExchangeRobotNormal) robot).start();
					}else{
						logger.info("?????????????????????: " + coinName);
						new Thread((ExchangeRobotCustom) robot).start();
					}
				}else{
					logger.info("???????????????????????????: " + coinName);
				}

			}
		}
		
		logger.info("?????????????????????" +(haltRobotList.size() + runRobotList.size()));
		
		if(errorRobotList.size() > 0) {
			String eStr = StringUtils.join(errorRobotList, ",");
			// ????????????
			String[] adminList = admins.split(",");
//			for(int i = 0; i < adminList.length; i++) {
//				try {
//					sendEmailMsg(adminList[i], "???????????????( ???" + errorRobotList.size()+ "??? ) : " + eStr, "???????????????????????????");
//				} catch (MessagingException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				} catch (TemplateException e) {
//					e.printStackTrace();
//				}
//			}
		}
	}
	
	@Async
    public void sendEmailMsg(String email, String msg, String subject) throws MessagingException, IOException, TemplateException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setTo(email);
        helper.setSubject(company + "-" + subject);
        Map<String, Object> model = new HashMap<>(16);
        model.put("msg", msg);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        Template template = cfg.getTemplate("simpleMessage.ftl");
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        helper.setText(html, true);

        //????????????
        javaMailSender.send(mimeMessage);
    }
}
