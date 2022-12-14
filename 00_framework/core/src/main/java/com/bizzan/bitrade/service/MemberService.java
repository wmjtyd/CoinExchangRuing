package com.bizzan.bitrade.service;

import com.bizzan.bitrade.constant.CertifiedBusinessStatus;
import com.bizzan.bitrade.constant.CommonStatus;
import com.bizzan.bitrade.dao.MemberDao;
import com.bizzan.bitrade.dao.MemberSignRecordDao;
import com.bizzan.bitrade.dao.MemberTransactionDao;
import com.bizzan.bitrade.entity.*;
import com.bizzan.bitrade.exception.AuthenticationException;
import com.bizzan.bitrade.pagination.Criteria;
import com.bizzan.bitrade.pagination.PageResult;
import com.bizzan.bitrade.pagination.Restrictions;
import com.bizzan.bitrade.service.Base.BaseService;
import com.bizzan.bitrade.util.BigDecimalUtils;
import com.bizzan.bitrade.util.GoogleAuthenticatorUtil;
import com.bizzan.bitrade.util.Md5;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bizzan.bitrade.constant.TransactionType.ACTIVITY_AWARD;

@Service
public class MemberService extends BaseService {

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private MemberSignRecordDao signRecordDao;

    @Autowired
    private MemberTransactionDao transactionDao;
    @Autowired
    private LocaleMessageSourceService messageSourceService;

    public Map<Long, Member> mapByMemberIds(List<Long> memberIds) {

        Map<Long, Member> map = new HashMap<>();
        List<Member> allByIdIn = memberDao.findAllByIdIn(memberIds);
        allByIdIn.forEach(v -> {
            map.put(v.getId(), v);
        });

        return map;
    }

    /**
     * ?????????????????? pageNo pageSize ??????????????????
     *
     * @param booleanExpressionList
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Transactional(readOnly = true)
    public PageResult<Member> queryWhereOrPage(List<BooleanExpression> booleanExpressionList, Integer pageNo, Integer pageSize) {
        List<Member> list;
        JPAQuery<Member> jpaQuery = queryFactory.selectFrom(QMember.member)
                .where(booleanExpressionList.toArray(new BooleanExpression[booleanExpressionList.size()]));
        jpaQuery.orderBy(QMember.member.id.desc());
        if (pageNo != null && pageSize != null) {
            list = jpaQuery.offset((pageNo - 1) * pageSize).limit(pageSize).fetch();
        } else {
            list = jpaQuery.fetch();
        }
        return new PageResult<>(list, jpaQuery.fetchCount());
    }

    public Member save(Member member) {
        return memberDao.save(member);
    }

    public Member saveAndFlush(Member member) {
        return memberDao.saveAndFlush(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public Member loginWithToken(String token, String ip, String device) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        //Member mr = memberDao.findMemberByTokenAndTokenExpireTimeAfter(token,new Date());
        Member mr = memberDao.findMemberByToken(token);
        return mr;
    }

    public Member login(String username, String password) throws Exception {
        Member member = memberDao.findMemberByMobilePhoneOrEmail(username, username);
        if (member == null) {
            throw new AuthenticationException(messageSourceService.getMessage("USER_PASSWORD_ERROR"));
        } else if (!Md5.md5Digest(password + member.getSalt()).toLowerCase().equals(member.getPassword())) {
            throw new AuthenticationException(messageSourceService.getMessage("USER_PASSWORD_ERROR"));
        } else if (member.getStatus().equals(CommonStatus.ILLEGAL)) {
            throw new AuthenticationException("ACCOUNT_ACTIVATION_DISABLED");
        }
        return member;
    }

    public Member loginWithCode(String username, String password,Long code) throws Exception {
        Member member = memberDao.findMemberByMobilePhoneOrEmail(username, username);
        if (member == null) {
            throw new AuthenticationException("?????????????????????");
        } else if (!Md5.md5Digest(password + member.getSalt()).toLowerCase().equals(member.getPassword())) {
            throw new AuthenticationException("?????????????????????");
        } else if (member.getStatus().equals(CommonStatus.ILLEGAL)) {
            throw new AuthenticationException("????????????????????????/??????????????????????????????");
        }else if(member.getGoogleState()!=null && member.getGoogleState().intValue()==1){
            //????????????
            long t = System.currentTimeMillis();
            GoogleAuthenticatorUtil ga = new GoogleAuthenticatorUtil();
            //  ga.setWindowSize(0); // should give 5 * 30 seconds of grace...
            boolean r = ga.check_code(member.getGoogleKey(), code, t);
            System.out.println("rrrr="+r);
            if(!r){
                throw new AuthenticationException("Google???????????????");
            }
        }
        return member;
    }
    /**
     * @author Hevin QQ:390330302 E-mail:bizzanex@gmail.com
     * @description
     * @date 2019/12/25 18:42
     */
    public Member findOne(Long id) {
        return memberDao.findOne(id);
    }

    /**
     * @author Hevin QQ:390330302 E-mail:bizzanex@gmail.com
     * @description ??????????????????
     * @date 2019/12/25 18:43
     */
    @Override
    public List<Member> findAll() {
        return memberDao.findAll();
    }

    public List<Member> findPromotionMember(Long id) {
        return memberDao.findAllByInviterId(id);
    }
    
    public Page<Member> findPromotionMemberPage(Integer pageNo, Integer pageSize,Long id){
        Sort orders = Criteria.sortStatic("id");
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);

        Criteria<Member> specification = new Criteria<Member>();
        specification.add(Restrictions.eq("inviterId", id, false));
        return memberDao.findAll(specification, pageRequest);
    }

    /**
     * @author Hevin QQ:390330302 E-mail:bizzanex@gmail.com
     * @description ??????
     * @date 2019/1/12 15:35
     */
    public Page<Member> page(Integer pageNo, Integer pageSize, CommonStatus status) {
        //???????????? (???????????? ??????    Criteria.sort("id","createTime.desc") ) //???????????????????????????
        Sort orders = Criteria.sortStatic("id");
        //????????????
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        //????????????
        Criteria<Member> specification = new Criteria<Member>();
        specification.add(Restrictions.eq("status", status, false));
        return memberDao.findAll(specification, pageRequest);
    }
    
    public Page<Member> findByPage(Integer pageNo, Integer pageSize) {
        //???????????? (???????????? ??????    Criteria.sort("id","createTime.desc") ) //???????????????????????????
        Sort orders = Criteria.sortStatic("id");
        //????????????
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        //????????????
        Criteria<Member> specification = new Criteria<Member>();
        return memberDao.findAll(specification, pageRequest);
    }

    public boolean emailIsExist(String email) {
        List<Member> list = memberDao.getAllByEmailEquals(email);
        return list.size() > 0 ? true : false;
    }

    public boolean usernameIsExist(String username) {
        return memberDao.getAllByUsernameEquals(username).size() > 0 ? true : false;
    }

    public boolean phoneIsExist(String phone) {
        return memberDao.getAllByMobilePhoneEquals(phone).size() > 0 ? true : false;
    }

    public Member findByUsername(String username) {
        return memberDao.findByUsername(username);
    }

    public Member findByEmail(String email) {
        return memberDao.findMemberByEmail(email);
    }

    public Member findByPhone(String phone) {
        return memberDao.findMemberByMobilePhone(phone);
    }

    public Page<Member> findAll(Predicate predicate, Pageable pageable) {
        return memberDao.findAll(predicate, pageable);
    }

    public String findUserNameById(long id) {
        return memberDao.findUserNameById(id);
    }

    //????????????
    @Transactional(rollbackFor = Exception.class)
    public void signInIncident(Member member, MemberWallet memberWallet, Sign sign) {
        member.setSignInAbility(false);//??????????????????
        memberWallet.setBalance(BigDecimalUtils.add(memberWallet.getBalance(), sign.getAmount()));//????????????

        // ????????????
        signRecordDao.save(new MemberSignRecord(member, sign));
        //????????????
        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setMemberId(member.getId());
        memberTransaction.setAmount(sign.getAmount());
        memberTransaction.setType(ACTIVITY_AWARD);
        memberTransaction.setSymbol(sign.getCoin().getUnit());
        transactionDao.save(memberTransaction);
    }

    //??????????????????
    public void resetSignIn() {
        memberDao.resetSignIn();
    }

    public void updateCertifiedBusinessStatusByIdList(List<Long> idList) {
        memberDao.updateCertifiedBusinessStatusByIdList(idList, CertifiedBusinessStatus.DEPOSIT_LESS);
    }

    /**
     * ???????????????????????????
     * @param promotion
     * @return
     */
    public boolean userPromotionCodeIsExist(String promotion) {
        return memberDao.getAllByPromotionCodeEquals(promotion).size() > 0 ? true : false;
    }
    
    public Long getMaxId() {
    	return memberDao.getMaxId();
    }

	public Member findMemberByPromotionCode(String code) {
		return memberDao.findMemberByPromotionCode(code);
	}

    public List<Member> findSuperPartnerMembersByIds(String uppers) {
        String[] idss = uppers.split(",");
        List<Long> ids = new ArrayList<>();
        for(String id:idss){
            ids.add(Long.parseLong(id));
        }
        return memberDao.findSuperPartnerMembersByIds(ids);
    }
    public List<Member> findAllByIds(String uppers) {
        String[] idss = uppers.split(",");
        List<Long> ids = new ArrayList<>();
        for(String id:idss){
            ids.add(Long.parseLong(id));
        }
        return memberDao.findAllByIds(ids);
    }

    public List<Member> findByInviterId(Long userId) {
        return memberDao.findByInviterId(userId);
    }

    public void updatePromotionCode(Long id, String promotionCode) {
        memberDao.updatePromotionCode(id,promotionCode);
    }
}
