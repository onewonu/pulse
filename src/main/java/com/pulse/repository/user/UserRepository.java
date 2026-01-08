package com.pulse.repository.user;

import com.pulse.entity.user.ProviderType;
import com.pulse.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderTypeAndProviderId(ProviderType providerType, String providerId);
}
