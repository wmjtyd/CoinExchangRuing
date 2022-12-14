package com.bizzan.bitrade.controller;

import com.alibaba.fastjson.JSONObject;
import com.bizzan.bitrade.constant.MemberLevelEnum;
import com.bizzan.bitrade.constant.PromotionLevel;
import com.bizzan.bitrade.constant.SysConstant;
import com.bizzan.bitrade.controller.BaseController;
import com.bizzan.bitrade.entity.Member;
import com.bizzan.bitrade.entity.MemberInviteStastic;
import com.bizzan.bitrade.entity.MemberInviteStasticRank;
import com.bizzan.bitrade.entity.MemberPromotion;
import com.bizzan.bitrade.entity.MemberWallet;
import com.bizzan.bitrade.entity.PromotionCard;
import com.bizzan.bitrade.entity.PromotionCardOrder;
import com.bizzan.bitrade.entity.PromotionMember;
import com.bizzan.bitrade.entity.PromotionRewardRecord;
import com.bizzan.bitrade.entity.RewardRecord;
import com.bizzan.bitrade.entity.transform.AuthMember;
import com.bizzan.bitrade.service.CoinService;
import com.bizzan.bitrade.service.MemberInviteStasticService;
import com.bizzan.bitrade.service.MemberPromotionService;
import com.bizzan.bitrade.service.MemberService;
import com.bizzan.bitrade.service.MemberWalletService;
import com.bizzan.bitrade.service.PromotionCardOrderService;
import com.bizzan.bitrade.service.PromotionCardService;
import com.bizzan.bitrade.service.RewardRecordService;
import com.bizzan.bitrade.util.DateUtil;
import com.bizzan.bitrade.util.GeneratorUtil;
import com.bizzan.bitrade.util.MessageResult;
import org.springframework.util.StringUtils;

import org.apache.shiro.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bizzan.bitrade.constant.SysConstant.SESSION_MEMBER;
import static com.bizzan.bitrade.util.MessageResult.error;
import static com.bizzan.bitrade.util.MessageResult.success;
/**
 * ??????
 *
 * @author Hevin QQ:390330302 E-mail:bizzanex@gmail.com
 * @date 2020???03???19???
 */
@RestController
@RequestMapping(value = "/promotion")
public class PromotionController extends BaseController{

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RewardRecordService rewardRecordService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private MemberInviteStasticService memberInviteStasticService;

    @Autowired
    private PromotionCardService promotionCardService;

    @Autowired
    private PromotionCardOrderService promotionCardOrderService;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberPromotionService memberPromotionService;

    private Random rand = new Random();
    /**
     * ???????????????????????????
     * @param member
     * @return
     */
    @RequestMapping(value = "/mypromotion")
    public MessageResult myPromotioin(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
        MemberInviteStastic result  =  memberInviteStasticService.findByMemberId(member.getId());
        if(result != null) {
            return success(result);
        }else {
            return error("no data");
        }
    }

    /**
     * ????????????
     * @param top
     * @return
     */
    @RequestMapping(value = "/weektoprank")
    public MessageResult topRankWeek(@RequestParam(value = "top", defaultValue = "20") Integer top) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        JSONObject result = (JSONObject) valueOperations.get(SysConstant.MEMBER_PROMOTION_TOP_RANK_WEEK + top);
        if (result != null){
            return success(result);
        } else {
            JSONObject resultObj = new JSONObject();
            // ??????
            List<MemberInviteStasticRank> topInviteWeek = memberInviteStasticService.topInviteCountByType(1, 20);
            for(MemberInviteStasticRank item3: topInviteWeek) {
                item3.setUserIdentify(item3.getUserIdentify().substring(0, 3) + "****" + item3.getUserIdentify().substring(item3.getUserIdentify().length() - 4, item3.getUserIdentify().length()));
            }

            resultObj.put("topinviteweek", topInviteWeek);

            valueOperations.set(SysConstant.MEMBER_PROMOTION_TOP_RANK_WEEK+top, resultObj, SysConstant.MEMBER_PROMOTION_TOP_RANK_EXPIRE_TIME_WEEK, TimeUnit.SECONDS);
            return success(resultObj);
        }
    }

    /**
     * ????????????
     * @param top
     * @return
     */
    @RequestMapping(value = "/monthtoprank")
    public MessageResult topRankMonth(@RequestParam(value = "top", defaultValue = "20") Integer top) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        JSONObject result = (JSONObject) valueOperations.get(SysConstant.MEMBER_PROMOTION_TOP_RANK_MONTH + top);
        if (result != null){
            return success(result);
        } else {
            JSONObject resultObj = new JSONObject();
            // ??????
            List<MemberInviteStasticRank> topInviteMonth = memberInviteStasticService.topInviteCountByType(2, 20);
            for(MemberInviteStasticRank item4: topInviteMonth) {
                item4.setUserIdentify(item4.getUserIdentify().substring(0, 3) + "****" + item4.getUserIdentify().substring(item4.getUserIdentify().length() - 4, item4.getUserIdentify().length()));
            }
            resultObj.put("topinvitemonth", topInviteMonth);

            valueOperations.set(SysConstant.MEMBER_PROMOTION_TOP_RANK_MONTH+top, resultObj, SysConstant.MEMBER_PROMOTION_TOP_RANK_EXPIRE_TIME_MONTH, TimeUnit.SECONDS);
            return success(resultObj);
        }
    }
    /**
     * ???????????????top??????????????? & ???top???????????????
     * @param member
     * @param top
     * @return
     */
    @RequestMapping(value = "/toprank")
    public MessageResult topRank(@RequestParam(value = "top", defaultValue = "20") Integer top) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        JSONObject result = (JSONObject) valueOperations.get(SysConstant.MEMBER_PROMOTION_TOP_RANK + top);
        if (result != null){
            return success(result);
        } else {
            JSONObject resultObj = new JSONObject();
            List<MemberInviteStastic> topReward = memberInviteStasticService.topRewardAmount(top);

            for(MemberInviteStastic item1 : topReward) {
                if(!StringUtils.isEmpty(item1.getUserIdentify())) {
                    item1.setUserIdentify(item1.getUserIdentify().substring(0, 3) + "****" + item1.getUserIdentify().substring(item1.getUserIdentify().length() - 4, item1.getUserIdentify().length()));
                }
                item1.setMemberId(item1.getMemberId() * (item1.getMemberId() % 100)); // ????????????????????????ID
            }

            List<MemberInviteStastic> topInvite = memberInviteStasticService.topInviteCount(top);
            for(MemberInviteStastic item2 : topInvite) {
                if(!StringUtils.isEmpty(item2.getUserIdentify())) {
                    item2.setUserIdentify(item2.getUserIdentify().substring(0, 3) + "****" + item2.getUserIdentify().substring(item2.getUserIdentify().length() - 4, item2.getUserIdentify().length()));
                }
                item2.setMemberId(item2.getMemberId() * (item2.getMemberId() % 100));
            }
            resultObj.put("topreward", topReward);
            resultObj.put("topinvite", topInvite);

            // ??????
            List<MemberInviteStasticRank> topInviteWeek = memberInviteStasticService.topInviteCountByType(1, 20);
            for(MemberInviteStasticRank item3: topInviteWeek) {
                if(!StringUtils.isEmpty(item3.getUserIdentify())) {
                    item3.setUserIdentify(item3.getUserIdentify().substring(0, 3) + "****" + item3.getUserIdentify().substring(item3.getUserIdentify().length() - 4, item3.getUserIdentify().length()));
                }
                item3.setMemberId(item3.getMemberId() * (item3.getMemberId() % 100));
            }

            // ??????
            List<MemberInviteStasticRank> topInviteMonth = memberInviteStasticService.topInviteCountByType(2, 20);
            for(MemberInviteStasticRank item4: topInviteMonth) {
                if(!StringUtils.isEmpty(item4.getUserIdentify())) {
                    item4.setUserIdentify(item4.getUserIdentify().substring(0, 3) + "****" + item4.getUserIdentify().substring(item4.getUserIdentify().length() - 4, item4.getUserIdentify().length()));
                }
                item4.setMemberId(item4.getMemberId() * (item4.getMemberId() % 100));
            }
            resultObj.put("topinviteweek", topInviteWeek);
            resultObj.put("topinvitemonth", topInviteMonth);

            valueOperations.set(SysConstant.MEMBER_PROMOTION_TOP_RANK+top, resultObj, SysConstant.MEMBER_PROMOTION_TOP_RANK_EXPIRE_TIME, TimeUnit.SECONDS);
            return success(resultObj);
        }
    }

    /**
     * ??????????????????
     *
     * @param member
     * @return
     */
//    @RequestMapping(value = "/record")
//    public MessageResult promotionRecord(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
//        List<Member> list = memberService.findPromotionMember(member.getId());
//        List<PromotionMember> list1 = list.stream().map(x ->
//                PromotionMember.builder().createTime(x.getRegistrationTime())
//                        .level(PromotionLevel.ONE)
//                        .username(x.getUsername())
//                        .build()
//        ).collect(Collectors.toList());
//        if (list.size() > 0) {
//            list.stream().forEach(x -> {
//                if (x.getPromotionCode() != null) {
//                    list1.addAll(memberService.findPromotionMember(x.getId()).stream()
//                            .map(y ->
//                                    PromotionMember.builder().createTime(y.getRegistrationTime())
//                                            .level(PromotionLevel.TWO)
//                                            .username(y.getUsername())
//                                            .build()
//                            ).collect(Collectors.toList()));
//                }
//            });
//        }
//        MessageResult messageResult = MessageResult.success();
//        messageResult.setData(list1.stream().sorted((x, y) -> {
//            if (x.getCreateTime().after(y.getCreateTime())) {
//                return -1;
//            } else {
//                return 1;
//            }
//        }).collect(Collectors.toList()));
//        return messageResult;
//    }
    @RequestMapping(value = "/record")
    public MessageResult promotionRecord2(@SessionAttribute(SESSION_MEMBER) AuthMember member, @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        //????????????????????????
        Page<Member> pageList = memberService.findPromotionMemberPage(pageNo-1, pageSize, member.getId());
        MessageResult messageResult = MessageResult.success();
        List<Member> list = pageList.getContent();
        List<PromotionMember> list1 = list.stream().map(x ->
                PromotionMember.builder().createTime(x.getRegistrationTime())
                        .level(PromotionLevel.ONE)
                        .username(x.getUsername().substring(0, 3) + "****" + x.getUsername().substring(x.getUsername().length() - 4, x.getUsername().length()))
                        .realNameStatus(x.getRealNameStatus())
                        .build()
        ).collect(Collectors.toList());

        messageResult.setData(list1.stream().sorted((x, y) -> {
            if (x.getCreateTime().after(y.getCreateTime())) {
                return -1;
            } else {
                return 1;
            }
        }).collect(Collectors.toList()));

        messageResult.setTotalPage(pageList.getTotalPages() + "");
        messageResult.setTotalElement(pageList.getTotalElements() + "");
        return messageResult;
    }


    /**
     * ??????????????????
     *
     * @param member
     * @return
     */
//    @RequestMapping(value = "/reward/record")
//    public MessageResult rewardRecord(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
//        List<RewardRecord> list = rewardRecordService.queryRewardPromotionList(memberService.findOne(member.getId()));
//        MessageResult result = MessageResult.success();
//        result.setData(list.stream().map(x ->
//                PromotionRewardRecord.builder().amount(x.getAmount())
//                        .createTime(x.getCreateTime())
//                        .remark(x.getRemark())
//                        .symbol(x.getCoin().getUnit())
//                        .build()
//        ).collect(Collectors.toList()));
//        return result;
//    }


    /**
     * ?????????????????????
     *
     * @param member
     * @return
     */
    @RequestMapping(value = "/reward/record")
    public MessageResult rewardRecord2(@SessionAttribute(SESSION_MEMBER) AuthMember member, @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<RewardRecord> pageList = rewardRecordService.queryRewardPromotionPage(pageNo, pageSize, memberService.findOne(member.getId()));
        MessageResult result = MessageResult.success();
        List<RewardRecord> list = pageList.getContent();
        result.setData(list.stream().map(x ->
                PromotionRewardRecord.builder().amount(x.getAmount())
                        .createTime(x.getCreateTime())
                        .remark(x.getRemark())
                        .symbol(x.getCoin().getUnit())
                        .build()
        ).collect(Collectors.toList()));

        result.setTotalPage(pageList.getTotalPages() + "");
        result.setTotalElement(pageList.getTotalElements() + "");
        return result;
    }

    /**
     * ????????????????????????BTC: 0.001)
     * @param member
     * @return
     */
    @RequestMapping(value = "/promotioncard/getfreecard")
    public MessageResult createFreeCard(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
        // ????????????????????????
        Member authMember = memberService.findOne(member.getId());
        if(authMember.getMemberLevel()== MemberLevelEnum.GENERAL){
            return MessageResult.error(500,"????????????????????????");
        }
        // ???????????????????????????
        List<PromotionCard> result = promotionCardService.findAllByMemberIdAndIsFree(member.getId(), 1);
        if(result != null && result.size() > 0) {
            return MessageResult.error(500,"????????????????????????????????????");
        }

        PromotionCard card = new PromotionCard();
        card.setCardName("??????????????????");
        card.setCardNo(authMember.getPromotionCode() + GeneratorUtil.getNonceString(5).toUpperCase());
        card.setAmount(new BigDecimal(0.001));
        card.setCardDesc("");
        card.setCoin(coinService.findByUnit("BTC"));
        card.setCount(30);
        card.setMemberId(authMember.getId());
        card.setIsFree(1);
        card.setIsEnabled(1);
        card.setExchangeCount(0);
        card.setTotalAmount(new BigDecimal(0.03));
        card.setIsLock(0);
        card.setLockDays(0);
        card.setIsEnabled(1);
        card.setCreateTime(DateUtil.getCurrentDate());

        PromotionCard cardResult = promotionCardService.save(card);

        return success(cardResult);
    }

    /**
     * ??????????????????????????????
     * @param member
     * @return
     */
    @RequestMapping(value = "/promotioncard/mycard")
    private MessageResult getMyCardList(@SessionAttribute(SESSION_MEMBER) AuthMember member) {

        List<PromotionCard> result = promotionCardService.findAllByMemberId(member.getId());

        return success(result);
    }

    /**
     * ???????????????
     * @param cardId
     * @return
     */
    @RequestMapping(value = "/promotioncard/detail")
    private MessageResult getCardDetail(@RequestParam(value = "cardId", defaultValue = "") Long cardId) {

        Assert.notNull(cardId, "?????????????????????");
        PromotionCard result = promotionCardService.findOne(cardId);
        Assert.notNull(result, "?????????????????????");

        return success(result);
    }
    /**
     * ??????????????????????????????????????????????????????????????????
     * @param member
     * @param cardNo
     * @return
     */
    @RequestMapping(value = "/promotioncard/exchangecard")
    @Transactional(rollbackFor = Exception.class)
    private MessageResult exhcangeCard(@SessionAttribute(SESSION_MEMBER) AuthMember member,
                                       @RequestParam(value = "cardNo", defaultValue = "") String cardNo) {

        // ????????????????????????
        Assert.notNull(cardNo, "????????????????????????");
        if(!StringUtils.hasText(cardNo)) {
            return error("????????????????????????");
        }
        PromotionCard card = promotionCardService.findPromotionCardByCardNo(cardNo);
        Assert.notNull(card, "????????????????????????");

        // ??????????????????
        Member authMember = memberService.findOne(member.getId());
        Assert.notNull(authMember, "????????????!");

        // ????????????????????????
        if(card.getIsEnabled() == 0) {
            return error("??????????????????");
        }

        // ????????????????????????
        MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(card.getCoin().getUnit(), authMember.getId());
        Assert.notNull(memberWallet, "??????????????????!");

        // ??????????????????????????????
        if(card.getExchangeCount() >= card.getCount()) {
            return error("????????????????????????");
        }
        // ???????????????????????????
        List<PromotionCardOrder> order = promotionCardOrderService.findByCardIdAndMemberId(card.getId(), authMember.getId());
        if(order != null && order.size() > 0) {
            return error("??????????????????????????????????????????");
        }

        // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        List<PromotionCardOrder> orderFree = promotionCardOrderService.findAllByMemberIdAndIsFree(authMember.getId(), 1);
        if(orderFree != null && orderFree.size() > 0) {
            return error("????????????????????????????????????????????????");
        }

        PromotionCardOrder newOrder= new PromotionCardOrder();
        newOrder.setMemberId(authMember.getId());
        newOrder.setAmount(card.getAmount());
        newOrder.setCard(card);
        newOrder.setIsFree(card.getIsFree());
        newOrder.setIsLock(card.getIsLock());
        newOrder.setLockDays(card.getLockDays());

        newOrder.setState(1);

        newOrder.setCreateTime(DateUtil.getCurrentDate());
        newOrder = promotionCardOrderService.save(newOrder);

        if(newOrder != null) {
            // ????????????????????????????????????????????????????????????
            if(authMember.getInviterId() == null) {
                if(authMember.getId() != card.getMemberId()) {
                    Member levelOneMember = memberService.findOne(card.getMemberId());
                    // ???????????????????????????????????????????????????????????????????????????
                    // ?????????????????????????????????????????????inviteID
                    authMember.setInviterId(card.getMemberId());
                    if(authMember.getMemberLevel() == MemberLevelEnum.REALNAME){
                        // ????????????????????????
                        MemberPromotion one = new MemberPromotion();
                        one.setInviterId(card.getMemberId());
                        one.setInviteesId(authMember.getId());
                        one.setLevel(PromotionLevel.ONE);
                        memberPromotionService.save(one);
                        // ?????????????????? + 1
                        levelOneMember.setFirstLevel(levelOneMember.getFirstLevel() + 1);

                        if(levelOneMember.getInviterId() != null) {
                            Member levelTwoMember = memberService.findOne(levelOneMember.getInviterId());
                            // ????????????????????????
                            MemberPromotion two = new MemberPromotion();
                            two.setInviterId(levelTwoMember.getId());
                            two.setInviteesId(authMember.getId());
                            two.setLevel(PromotionLevel.TWO);
                            memberPromotionService.save(two);

                            // ?????????????????? + 1
                            levelTwoMember.setSecondLevel(levelTwoMember.getSecondLevel() + 1);
                        }
                    }
                }
            }

            // ???????????????????????????
            memberWalletService.increaseFrozen(memberWallet.getId(), newOrder.getAmount());

            // ??????????????????
            card.setExchangeCount(card.getExchangeCount() + 1);
            promotionCardService.saveAndFlush(card);

            return success("???????????????");
        }else {
            return error("???????????????");
        }
    }
}
