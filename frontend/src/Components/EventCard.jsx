import React from 'react'
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import { fontWeight } from '@mui/system';
import dayjs from "dayjs";
import { getEventData, getToken } from '../Helpers';
import { CentredBox, UploadPhoto } from '../Styles/HelperStyles';
import { useNavigate } from 'react-router-dom';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';

export default function EventCard({event_id, tokenReq=false}) {
  const navigate = useNavigate()
  var utc = require('dayjs/plugin/utc')
  dayjs.extend(utc)
  const [event, setEvent] = React.useState({
    event_name: "",
    location: {
      street_no: "",
      street_name: "",
      postcode: "",
      state: "",
      country: ""
    },
    host_id: '',
    start_date: dayjs().toISOString(),
    end_date: dayjs().toISOString(),
    description: "",
    tags: [],
    admins: [],
    picture: "",
    host_id: ''
  })

  const [successLoad, setSuccessLoad] = React.useState(false)

  React.useEffect(() => {
    try {
      if(tokenReq) {
        getEventData(event_id, setEvent, getToken())
      } else {
        getEventData(event_id, setEvent)
      }
      setSuccessLoad(true)
    } catch(e) {
      console.log(e)
    }
  },[event_id])

  const handleClick = (e) => {
    e.stopPropagation();
    e.nativeEvent.stopImmediatePropagation();
    window.location.replace(`/view_event/${event_id}`)
  }
  return (
    <>
      {successLoad
        ? <Box
            sx={{
              width: '250px',
              height: '400px',
              backgroundColor: '#FFFFFF',
              borderRadius: '5px',
              '&:hover': {
                boxShadow: '4',
                cursor: 'pointer',
              },
              display: 'flex',
              flexDirection: 'column'
            }}
            onClick={handleClick}
          > 
            {(event.event_name === "")
              ? <Stack sx={{width: '100%', p: 1}} spacing={1}>
                  <Skeleton variant="rounded" width={230} height={"125px"} />
                  <Skeleton variant="text" sx={{ fontSize: '2rem', width: 200}} />
                  <Skeleton variant="text" sx={{ fontSize: '1rem', width: 200 }} />
                  <Skeleton variant="text" sx={{ fontSize: '1rem', width: 200 }} />
                </Stack>
              : <>
                  <CentredBox
                    sx={{
                      height: '125px',
                      backgroundColor: '#c9c9c9'
                    }}
                  >
                    {(event.picture === '')
                      ? <h3>Event Photo</h3>
                      : <UploadPhoto src={event.picture}/>
                    }
                  </CentredBox>
                  <Box
                    sx={{
                      padding: '5px'
                    }}
                  >
                    <Typography
                      sx={{
                        fontSize:"20px",
                        fontWeight: "bold"
                      }}
                    >
                      {event.event_name}
                    </Typography>
                    <Typography
                      sx={{
                        fontSize:"13px",
                        fontWeight: "regular",
                        color: "#AE759F",
                      }}
                    >
                      {dayjs(event.start_date).format('lll')} - {dayjs(event.end_date).format('lll')}
                    </Typography>
                    <Typography
                      sx={{
                        fontSize:"13px",
                        fontWeight: "light",
                      }}
                    >
                      {event.location.suburb}, {event.location.state}, {event.location.country}
                    </Typography>
                  </Box>
                </>
            }
          </Box>
        : <> </>
      }
    </>
  )
}