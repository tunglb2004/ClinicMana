# 🚀 Password Management Features - Deployment Guide

## Pre-Deployment Checklist

### Backend Requirements
- [ ] Spring Boot 3.x running
- [ ] MySQL database configured
- [ ] JWT configuration complete
- [ ] Password hashing enabled (BCrypt)
- [ ] OTP service implemented
- [ ] Email service configured (if OTP via email)
- [ ] CORS properly configured

### Frontend Setup
- [ ] Node.js 18+ installed
- [ ] All dependencies installed: `npm install`
- [ ] Environment variables configured
- [ ] TypeScript compilation passes: `npm run build`
- [ ] No linting errors: `npm run lint`

### Environment Configuration

**Development (.env.local)**
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**Staging (.env.staging)** 
```env
NEXT_PUBLIC_API_URL=https://staging-api.clinic.com
```

**Production (.env.production)**
```env
NEXT_PUBLIC_API_URL=https://api.clinic.com
```

---

## Deployment Steps

### Step 1: Verify Code
```bash
# Install dependencies
npm install

# Check TypeScript
npm run build

# Check linting
npm run lint
```

### Step 2: Build Frontend
```bash
# Production build
npm run build

# This creates: .next/ directory with optimized build
```

### Step 3: Start Server
```bash
# Start Next.js server
npm start

# Or in development
npm run dev
```

### Step 4: Test Features

#### Reset Password Flow
1. Navigate to `http://yoursite.com/reset-password`
2. Enter your email
3. Click "Gửi mã OTP"
4. Wait for OTP (check backend logs)
5. Enter OTP code
6. Set new password
7. Verify redirect to login

#### Change Password Flow
1. Login to the application
2. Navigate to `http://yoursite.com/change-password`
3. Enter current password
4. Enter new password
5. Confirm new password
6. Click submit
7. Verify redirect to dashboard

#### Get User Info
1. Login successfully
2. Check if user info loads correctly
3. Verify in browser DevTools Network tab

---

## Verification Endpoints

### Test API Connectivity
```bash
# Test backend API is accessible
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <your_token>"

# Expected response
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "roles": ["PATIENT"],
  ...
}
```

### Test OTP Service
```bash
# Send OTP
curl -X POST http://localhost:8080/api/auth/register/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "type": "reset"}'

# Expected response
{
  "message": "OTP sent successfully",
  "email": "test@example.com",
  "expiresInMinutes": 10
}
```

---

## Troubleshooting

### Issue: "API URL not defined"
**Solution**: Check `.env.local` has `NEXT_PUBLIC_API_URL`

### Issue: "OTP not received"
**Solution**: 
- Check backend OTP service is running
- Check email configuration
- Check OTP expiration time

### Issue: "Password change returns 401"
**Solution**:
- Verify authorization token is valid
- Check token not expired
- Ensure user is authenticated

### Issue: "Form validation not working"
**Solution**:
- Check browser console for errors
- Verify JavaScript enabled
- Clear browser cache

### Issue: "Redirect not working"
**Solution**:
- Check Next.js routing is working
- Verify page files exist
- Check browser console for errors

---

## Performance Optimization

### Frontend Optimization
```bash
# Enable compression
npm run build

# This optimizes:
# - JavaScript minification
# - CSS optimization
# - Image optimization
# - Code splitting
```

### API Optimization
- Cache user info when possible
- Minimize API calls
- Use pagination for large responses

---

## Security Hardening

### HTTPS Requirement
- [ ] Enable HTTPS in production
- [ ] Use valid SSL certificate
- [ ] Redirect HTTP to HTTPS

### CORS Configuration
```typescript
// On backend, configure CORS
corsConfiguration.allowCredentials(true)
  .allowedOrigins("https://yoursite.com")
  .allowedMethods("GET", "POST", "PUT", "DELETE")
  .allowedHeaders("Content-Type", "Authorization")
```

### Rate Limiting
```
// Add rate limiting (backend)
- Max 5 OTP requests per email per hour
- Max 3 login attempts per minute
- Max 10 password reset attempts per day
```

### CSRF Protection
- [ ] Verify CSRF tokens are used
- [ ] Ensure SameSite cookie attribute set

---

## Monitoring & Logging

### Metrics to Monitor
- Password reset success rate
- Password change success rate
- OTP verification success rate
- Failed login attempts
- API response times

### Logs to Review
```
// Frontend
- Console errors
- Network errors
- Form submission errors

// Backend
- Authentication failures
- OTP generation/verification
- Password update success/failure
- API request/response times
```

### Error Tracking
- Implement error boundary component
- Send errors to monitoring service
- Set up alerts for critical errors

---

## Post-Deployment Verification

### User Acceptance Testing
- [ ] Reset password works end-to-end
- [ ] Change password works end-to-end
- [ ] User info displays correctly
- [ ] Error messages are helpful
- [ ] Forms are responsive on mobile
- [ ] Navigation links work correctly

### Performance Testing
- [ ] Page load times acceptable
- [ ] Form submission times acceptable
- [ ] API response times < 2 seconds
- [ ] No memory leaks
- [ ] No excessive re-renders

### Security Testing
- [ ] HTTPS enforced
- [ ] XSS protection working
- [ ] CSRF protection working
- [ ] Token expiration handled
- [ ] No password exposure

---

## Rollback Plan

If issues occur post-deployment:

### Quick Rollback
```bash
# Rollback to previous version
git revert <commit_hash>
npm run build
npm start
```

### Manual Rollback
1. Restore previous `.env` files
2. Restore previous code from git
3. Restart application
4. Verify rollback successful

### Data Rollback
- User table is not modified
- Password changes are logged
- No data cleanup needed
- All changes reversible

---

## Maintenance

### Regular Tasks
- [ ] Monitor error logs daily
- [ ] Check API response times
- [ ] Verify OTP service is working
- [ ] Update dependencies monthly
- [ ] Review security logs monthly

### Quarterly Tasks
- [ ] Performance optimization review
- [ ] Security audit
- [ ] Load testing
- [ ] Backup verification
- [ ] Documentation update

### Yearly Tasks
- [ ] Security penetration testing
- [ ] Architecture review
- [ ] Dependency updates
- [ ] Database optimization
- [ ] Disaster recovery drill

---

## Scaling Considerations

### Database Scaling
- Add indexes on frequently queried columns
- Implement caching for user info
- Archive old OTP records

### API Scaling
- Implement API caching headers
- Add load balancing
- Use CDN for static assets

### Frontend Scaling
- Use service workers for offline capability
- Implement lazy loading
- Optimize image sizes

---

## Deployment Timeline

### Before Deployment (1 week)
- [ ] Code review complete
- [ ] Testing complete
- [ ] Documentation complete
- [ ] Stakeholder approval

### Day of Deployment (2-4 hours)
- [ ] Backup production data
- [ ] Deploy to staging
- [ ] Run smoke tests
- [ ] Get final approval
- [ ] Deploy to production
- [ ] Monitor for errors

### After Deployment (24 hours)
- [ ] Monitor error rates
- [ ] Check user feedback
- [ ] Verify all features working
- [ ] Review performance metrics
- [ ] Prepare rollback if needed

---

## Success Criteria

✅ **Deployment Successful When:**
- All 3 features working without errors
- No critical security issues
- Response times < 2 seconds
- 99% of user sessions successful
- No data integrity issues
- Error rates < 0.1%

❌ **Rollback Triggers:**
- Critical security vulnerability found
- > 5% of users unable to reset password
- > 5% of users unable to change password
- Database corruption detected
- Service availability < 99%

---

## Support Contacts

- **Developers**: G2 Team dev@clinic.com
- **DevOps**: infrastructure@clinic.com  
- **Product**: product@clinic.com
- **Security**: security@clinic.com

---

## Documentation References

- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- [PASSWORD_MANAGEMENT.md](PASSWORD_MANAGEMENT.md)
- [COMPLETE_IMPLEMENTATION_CHECKLIST.md](COMPLETE_IMPLEMENTATION_CHECKLIST.md)
- [PASSWORD_FEATURES_INDEX.md](PASSWORD_FEATURES_INDEX.md)

---

**Deployment Date**: _______________
**Deployed By**: _______________
**Approved By**: _______________
**Status**: ✅ Ready for Production

---

Good luck with your deployment! 🚀
