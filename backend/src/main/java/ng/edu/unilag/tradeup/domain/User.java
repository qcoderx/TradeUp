package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.stream.Collectors;

/** A student (or moderator) with an account on TradeUp. */
@Entity
@Table(
        name = "users",
        indexes = {
            @Index(name = "idx_user_email", columnList = "email", unique = true),
            @Index(name = "idx_user_matric", columnList = "matricNumber", unique = true)
        })
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String matricNumber;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 80)
    private String department;

    /** Where on campus this student prefers to meet for a handover. */
    @Column(length = 80)
    private String campusLocation;

    @Column(length = 400)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    /** Completed trades, used to build visible trust on a profile. */
    @Column(nullable = false)
    private int completedTrades = 0;

    @Column(nullable = false)
    private boolean active = true;

    protected User() {
        // Required by JPA.
    }

    public User(String email, String matricNumber, String fullName, String passwordHash, String department) {
        this.email = email;
        this.matricNumber = matricNumber;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.department = department;
    }

    /**
     * Initials for the avatar fallback: "Adebowale Okiki David" becomes "AD".
     * Falls back to a single letter for mononyms, and to "?" for a blank name.
     */
    public String initials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\s+");
        String joined = Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .map(p -> String.valueOf(Character.toUpperCase(p.charAt(0))))
                .collect(Collectors.joining());
        return joined.length() <= 2 ? joined : joined.substring(0, 1) + joined.substring(joined.length() - 1);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void recordCompletedTrade() {
        this.completedTrades++;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatricNumber() {
        return matricNumber;
    }

    public void setMatricNumber(String matricNumber) {
        this.matricNumber = matricNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCampusLocation() {
        return campusLocation;
    }

    public void setCampusLocation(String campusLocation) {
        this.campusLocation = campusLocation;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getCompletedTrades() {
        return completedTrades;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
