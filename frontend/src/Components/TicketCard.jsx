import {Collapse, Divider, FormControl, FormHelperText, Grid, IconButton, InputAdornment, Skeleton, Tooltip, Typography } from '@mui/material'
import { Box } from '@mui/system'
import React from 'react'
import { CentredBox, UploadPhoto } from '../Styles/HelperStyles'
import dayjs from 'dayjs'
import { apiFetch, checkValidEmail, getEventData, getToken, getUserData } from '../Helpers'
import EmailIcon from '@mui/icons-material/Email'
import { ContrastInputNoOutline, ContrastInputWrapper } from '../Styles/InputStyles'
import SendIcon from '@mui/icons-material/Send';
import GroupsIcon from '@mui/icons-material/Groups';
import { useNavigate } from 'react-router-dom'
import MoreVertIcon from '@mui/icons-material/MoreVert';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';

export default function TicketCard({event, ticket_id, ticketOwner}) {
  const navigate = useNavigate

  const [ticketDetails, setTicketDetails] = React.useState(null)
  const [sectionSeating, setSectionSeating] = React.useState(false)
  const [userData, setUserData] = React.useState(null)
  const [sectionName, setSectionName] = React.useState('')
  const [sendEmail, setSendEmail] = React.useState('')
  const [openInput, setOpenInput] = React.useState(false)
  const [helperMsg, setHelperMsg] = React.useState('')
  const [anchorEl, setAnchorEl] = React.useState(null);

  const open = Boolean(anchorEl)
  const handleClickMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };

  // Get Ticket Data
  React.useEffect(()=> {
    if (event !== null) {
      getTicketData()  
    }
  },[event])

  const getTicketData = async () => {
    // Send API call to get ticket details
    const paramsObj = {
      ticket_id: ticket_id,
    }
    const searchParams = new URLSearchParams(paramsObj)
    try {
      const response = await apiFetch('GET', `/api/ticket/view?${searchParams}`)
      setTicketDetails(response)
      const name = response.section.trim()
      if (name.split(' ').length > 1) {
        const names = name.split(' ')
        setSectionName(names[0][0]+names[1][0])
      } else {
        setSectionName(name)
      }
      event.seating_details.forEach(function(section) {
        if (section.section === response.section) {
          setSectionSeating(section.has_seats)
          return
        }
      })
    } catch (e) {
      console.log(e)
    }
    
  }

  // Handle toggle of send email
  const handleToggle = () => {
    setOpenInput(!openInput);
  };

  // Handle email change
  const handleEmailChange = (e) => {
    setSendEmail(e.target.value)
    setHelperMsg("")
  }

  // Hanlde sending ticket to email
  const handleSendEmail = async () => {
    // Form Check
    if (!checkValidEmail(sendEmail)) {
      setHelperMsg("Invalid email")
      return
    }

    try {
      const body = {
        auth_token: getToken(),
        ticket_id: ticket_id,
        email: sendEmail
      }
      const response = await apiFetch('POST', '/api/ticket/send', body)
      setHelperMsg("Email sent")
    } catch (e) {
      console.log(e)
    }
  }

  const handleManageGroup = () => {
    window.location.replace(`/group/manage/${ticketDetails.group_id}`)
  }

  const handleRefund = () => {
    window.location.replace(`/ticket/refund/${ticket_id}`)
  }

  return (
    <Box sx={{boxShadow: 5, backgroundColor: '#FFFFFFF', m: 1, p: 3, borderRadius: 1}}>
      {(ticketDetails === null)
        ? <>
            <CentredBox sx={{flexDirection: 'column', width: '100%'}}>
              <Skeleton variant="rounded" width={560} height={400}/>
              <Skeleton variant="text"  width={360} sx={{ fontSize: 50 }} />
              <Skeleton variant="text"  width={260} sx={{ fontSize: 20 }} />
            </CentredBox>
            <br/>
            <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
              <CentredBox sx={{width: '100%', flexDirection: 'column'}}>
                <Skeleton variant="text" width={360} sx={{ fontSize: 30 }} />
                <CentredBox sx={{gap: 1}}>
                  <Skeleton variant="text" width={160} sx={{ fontSize: 20 }} />
                  <Divider orientation="vertical" flexItem />
                  <Skeleton variant="text" width={160} sx={{ fontSize: 20 }} />
                </CentredBox>
              </CentredBox>
            </Box>
          </>
        : <>
            <CentredBox sx={{flexDirection: 'column', width: '100%'}}>
              {(event.picture !== '')
                ? <UploadPhoto sx={{height: 560, width: 560}} src={event.picture}/>
                : <Box sx={{width: 560, height: 560, borderRadius: 5, backgroundColor: '#EEEEEE'}}>
                    <CentredBox sx={{height: '100%', alignItems: 'center'}}>
                      <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1, texAlign: 'center'}}>
                        {event.event_name}
                      </Typography>
                    </CentredBox>
                  </Box>
              }
              <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1, texAlign: 'center'}}>
                {event.event_name}
              </Typography>
              <Typography sx={{fontSize: 20, fontWeight: "regular", color: "#AE759F", texAlign: 'center'}}>
                {dayjs(event.start_date).format('lll')} - {dayjs(event.end_date).format('lll')}
              </Typography>
            </CentredBox>
            <br/>
            <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
              <Box sx={{width: '100%'}}>
                <Grid container>
                  <Grid item xs={1}>
                    <CentredBox sx={{height: '100%', alignItems: 'center'}}>
                      {(ticketDetails.group_id !== undefined)
                        ? <Tooltip title="Manage group ticket">
                            <IconButton onClick={handleManageGroup}>
                              <GroupsIcon/>
                            </IconButton>
                          </Tooltip>
                        : <></>
                      }
                    </CentredBox>
                  </Grid>
                  <Grid item xs={10}>
                    {(sectionSeating)
                      ? <Typography
                          sx={{
                            fontSize: 30,
                            fontWeight: 'bold',
                            textAlign: 'center'
                          }}
                        >
                          {sectionName}{ticketDetails.seat_num}
                        </Typography>
                      : <Typography
                          sx={{
                            fontSize: 30,
                            fontWeight: 'bold',
                            textAlign: 'center'
                          }}
                        >
                          {sectionName} x 1
                        </Typography>
                    }
                  </Grid>
                  <Grid item xs={1}>
                    {ticketOwner
                      ? <CentredBox sx={{height: '100%', alignItems: 'center'}}>
                          <Tooltip title="Ticket Menu">
                            <IconButton onClick={handleClickMenu}>
                              <MoreVertIcon/>
                            </IconButton>
                          </Tooltip>
                          <Menu
                            anchorEl={anchorEl}
                            open={open}
                            onClose={handleCloseMenu}
                            sx={{width: 300}}
                          >
                            <MenuItem sx={{width: '100%'}} onClick={handleRefund}>Refund Ticket</MenuItem>
                          </Menu>
                        </CentredBox>
                      : <></>
                    }
                  </Grid>
                  <Grid item xs={1} sx={{height: '100%'}}></Grid>
                  <Grid item xs={10}>
                    <CentredBox sx={{gap: 1, height: '100%'}}>
                      <Typography
                        sx={{
                          fontSize: 20,
                          textAlign: 'center'
                        }}
                      >
                        {ticketDetails.first_name} {ticketDetails.last_name}
                      </Typography>
                      <Divider orientation="vertical" flexItem />
                      <Typography
                        sx={{
                          fontSize: 20,
                          textAlign: 'center'
                        }}
                      >
                        {ticketDetails.email}
                      </Typography>
                    </CentredBox>
                  </Grid>
                  <Grid item xs={1}>
                    <CentredBox>
                      {ticketOwner
                        ? <Tooltip title="Send to Email">
                            <IconButton onClick={handleToggle}>
                              <EmailIcon fontSize='small'/>
                            </IconButton>
                          </Tooltip>
                        : <></>
                      }
                    </CentredBox>   
                  </Grid>
                </Grid>
                <Collapse in={openInput}>
                  <CentredBox sx={{pt: 1, width: '100%', }}>
                    <FormControl>
                      <ContrastInputWrapper sx={{width: '100%'}}>
                        <ContrastInputNoOutline
                          fullWidth
                          endAdornment={
                            <Tooltip title="Send">
                              <IconButton onClick={handleSendEmail}>
                                <SendIcon/>
                              </IconButton>
                            </Tooltip>
                          }
                          placeholder="Email"
                          onChange={handleEmailChange}
                        />
                      </ContrastInputWrapper>
                      <FormHelperText sx={{textAlign: 'center'}}>{helperMsg}</FormHelperText>
                    </FormControl>
                  </CentredBox>
                </Collapse>
              </Box>
            </Box>
          </>
      }
    </Box>
  )
}