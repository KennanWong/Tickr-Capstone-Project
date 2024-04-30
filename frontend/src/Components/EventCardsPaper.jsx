import React from 'react'
import { Box } from '@mui/system'
import EventCard from '../Components/EventCard';
import { Divider, Button, Typography } from '@mui/material';
import { CentredBox } from '../Styles/HelperStyles';

export default function EventCardsPaper({events, moreEvents, handleMoreEvents, noResultsText="No Results"}) {
  return (
    <Box sx={{display: 'flex', flexWrap:'wrap', borderRadius: 3, p:2, backgroundColor: '#F6F6F6', gap: 3}}>
      {(events.length > 0)
        ? <>
            {events.map((event, key) => {
              return (
                <EventCard event_id={event} key={key}/>
              )
            })}
            {moreEvents
              ? <CentredBox sx={{width: '100%'}}>
                  <Button sx={{textTransform: 'none', color: '#CCCCCC'}} variant='text'>
                    More Results
                  </Button>
                </CentredBox> 
              : <></>
            }
          </>
        : <CentredBox sx={{width: '100%'}}>
            <Typography sx={{fontWeight: 'bold', fontSize: 25, color: '#CCCCCC'}}>
              {noResultsText}
            </Typography>
          </CentredBox>
      }
      
    </Box>
  )
}