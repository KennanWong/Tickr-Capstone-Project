import React from 'react'
import { Alert, alpha, Collapse, FormControl, FormHelperText, Grid, IconButton, Tooltip, Typography } from "@mui/material";
import { Box } from "@mui/system";
import { CentredBox } from "../Styles/HelperStyles";
import { ContrastInput, ContrastInputNoOutline, ContrastInputWrapper } from "../Styles/InputStyles";
import AddIcon from '@mui/icons-material/Add';
import { apiFetch, checkValidEmail, getToken } from "../Helpers";
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';


export default function GroupMemberTicket({ticket, groupId, setTickets, tickets, index}) {

  const handleRemoveInvite = async (e) => {
    try {
      if (ticket.invite_id !== undefined) {
        const inviteRemove = {
          auth_token: getToken(),
          invite_id: ticket.invite_id,
          group_id: groupId
        }
        const inviteRemoveResponse = await apiFetch('DELETE', '/api/group/invite/remove', inviteRemove)
      } else {
        const memberRemove = {
          auth_token: getToken(),
          group_id: groupId,
          email: ticket.email
        }
        const memberRemoveResponse = await apiFetch('DELETE', '/api/group/remove', memberRemove)
      }

      // Remove ticket from list
      const tickets_t = tickets
      tickets_t.splice(index, 1)
      setTickets([...tickets_t])
    } catch (e) {
      console.log(e)
    }
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
                    {!ticket.purchased
                      ? <Tooltip title="Remove invite">
                          <IconButton onClick={handleRemoveInvite}>
                            <DeleteOutlineIcon/>
                          </IconButton>
                        </Tooltip>
                      : <></>
                    }
                  </>
                }
                placeholder="Email"
                disabled
                value={ticket.email}
              >
              </ContrastInputNoOutline>
            </ContrastInputWrapper>
          </Box>
        </Grid>
        <Grid item xs={1}>
        </Grid>
      </Grid>
    </Box>
  )
}