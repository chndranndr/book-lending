package com.library.booklending.member;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<MemberDtos.MemberResponse> create(@Valid @RequestBody MemberDtos.MemberRequest request) {
        MemberDtos.MemberResponse response = memberService.create(request);
        return ResponseEntity.created(URI.create("/api/members/" + response.id())).body(response);
    }

    @GetMapping
    public List<MemberDtos.MemberResponse> findAll() {
        return memberService.findAll();
    }

    @GetMapping("/{id}")
    public MemberDtos.MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @PutMapping("/{id}")
    public MemberDtos.MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberDtos.MemberRequest request) {
        return memberService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
