package com.library.booklending.member;

import com.library.booklending.common.BusinessErrorCode;
import com.library.booklending.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberDtos.MemberResponse create(MemberDtos.MemberRequest request) {
        return toResponse(memberRepository.save(new Member(request.name(), request.email())));
    }

    @Transactional(readOnly = true)
    public List<MemberDtos.MemberResponse> findAll() {
        return memberRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MemberDtos.MemberResponse findById(Long id) {
        return toResponse(getMember(id));
    }

    @Transactional
    public MemberDtos.MemberResponse update(Long id, MemberDtos.MemberRequest request) {
        Member member = getMember(id);
        member.update(request.name(), request.email());
        return toResponse(memberRepository.save(member));
    }

    @Transactional
    public void delete(Long id) {
        Member member = getMember(id);
        memberRepository.delete(member);
        memberRepository.flush();
    }

    @Transactional(readOnly = true)
    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Member not found",
                        "Member " + id + " does not exist",
                        BusinessErrorCode.MEMBER_NOT_FOUND
                ));
    }

    private MemberDtos.MemberResponse toResponse(Member member) {
        return new MemberDtos.MemberResponse(member.getId(), member.getName(), member.getEmail());
    }
}
