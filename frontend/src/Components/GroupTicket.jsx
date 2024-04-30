import { Alert, alpha, Collapse, FormControl, FormHelperText, Grid, IconButton, Tooltip, Typography } from "@mui/material";
import { Box } from "@mui/system";
import React from "react";
import { CentredBox } from "../Styles/HelperStyles";
import { ContrastInput, ContrastInputNoOutline, ContrastInputWrapper } from "../Styles/InputStyles";
import AddIcon from '@mui/icons-material/Add';
import { apiFetch, checkValidEmail, getToken } from "../Helpers";
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';

export default function GroupTicket({ticket, groupId}) {
  const [email, setEmail] = React.useState('')
  const [error, setError] = React.useState(false)
  const [helperMsg, setHelperMsg] = React.useState('')
  const [inviteSent, setInviteSent] = React.useState(false)

  const handleInvite = async () => {
    if (!checkValidEmail(email)) {
      setError(true)
      setHelperMsg('Invalid email')
      return
    }
    // Check if user exists
    try {     
      const verifyUser = await apiFetch('GET', `/api/user/search?email=${email}`, null)
    } catch (error) {
      console.log(error)
      setError(true)
      setHelperMsg(error.reason)
      return
    }

    
    // Send invite
    try {
      const body = {
        auth_token: getToken(),
        group_id: groupId,
        email: email,
        reserve_id: ticket.reserve_id
      }

      const response = await apiFetch ('POST', '/api/group/invite', body)
      setInviteSent(true)
    } catch (e) {
      console.log(e)
      setError(true)
      setHelperMsg(e.reason)
    }

  }

  const handleChange = (e) => {
    setEmail(e.target.value)
    setError(false)
    setHelperMsg('')
  }

  const handleRemoveInvite = async (e) => {
    // Get groups tickets 
    var groupDetails = ''
    try {
      const groupDetailbody = {
        auth_token: getToken(),
        group_id: groupId
      }
  
      const searchGDPParams = new URLSearchParams(groupDetailbody)
      groupDetails = await apiFetch('GET', `/api/group/details?${searchGDPParams}`, null)  
    } catch (e) {
      console.log(e)
      return
    }
    
    // removal flag
    var removed = false

    // Search if member is in group
    const groupMembers = groupDetails.group_members
    groupMembers.forEach(async function (member) {
      if (member.email === email) {
        try {
          const memberRemove = {
            auth_token: getToken(),
            group_id: groupId,
            email: email
          }
          const memberRemoveResponse = await apiFetch('DELETE', '/api/group/remove', memberRemove)
          removed = true
        } catch (e) {
          console.log(e)
        }
      }
    })

    // If not in group members, check within pending invites
    const pendingInvites = groupDetails.pending_invites
    if (!removed) {
      pendingInvites.forEach(async function (invite) {
        if (invite.email === email) {
          try {
            const inviteRemove = {
              auth_token: getToken(),
              invite_id: invite.invite_id,
              group_id: groupId
            }
            const inviteRemoveResponse = await apiFetch('DELETE', '/api/group/invite/remove', inviteRemove)
            removed = true
          } catch (e) {
            console.log(e)
          }
        }
      })
    }
    setEmail('')
    setInviteSent(false)
  }

  return (
    <Box sx={{width: '100%', pt: 2, pb: 2}}>
      <Grid container spacing={2}>
        <Grid item xs={1}></Grid>
        <Grid item xs={3}>
          <CentredBox sx={{backgroundColor: alpha('#6A7B8A', 0.3), height: '100%', width: '100%', borderRadius: 3}}>
            <Typography>
              {ticket.section[0]}{ticket.seat_number}
            </Typography>
          </CentredBox>
        </Grid>
        <Grid item xs={7}>
          <Box>
            <ContrastInputWrapper>
              <ContrastInputNoOutline
                fullWidth
                endAdornment={
                  <>
                    {inviteSent
                      ? <Tooltip title="Remove invite">
                          <IconButton onClick={handleRemoveInvite}>
                            <DeleteOutlineIcon/>
                          </IconButton>
                        </Tooltip>
                      : <Tooltip title="Send Invite">
                          <span>
                            <IconButton
                              disabled={email.length <= 0}
                              onClick={handleInvite}
                            >
                              <AddIcon/>
                            </IconButton>
                          </span>
                        </Tooltip>
                    }
                  </>
                }
                placeholder="Email"
                onChange={handleChange}
                error={error}
                disabled={inviteSent}
                value={email}
              >
              </ContrastInputNoOutline>
            </ContrastInputWrapper>
          </Box>
        </Grid>
        <Grid item xs={1}>
        </Grid>
      </Grid>
      <CentredBox sx={{pt: 1}}>
        <Collapse in={error}>
          <Alert severity="error">
            {helperMsg}
          </Alert>
        </Collapse>
      </CentredBox>
    </Box>
  )
}