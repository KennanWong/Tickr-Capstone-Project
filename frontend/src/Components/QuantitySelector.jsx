import { Checkbox, FormControl, Grid, MenuItem, Select, Tooltip, Typography } from '@mui/material'
import React from 'react'
import { CentredBox } from '../Styles/HelperStyles'
import Collapse from '@mui/material/Collapse';
import { styled } from '@mui/material/styles';
import IconButton from '@mui/material/IconButton';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PersonIcon from '@mui/icons-material/Person';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import { Box } from '@mui/system';
import { setFieldInState } from '../Helpers';

const ExpandMore = styled((props) => {
  const { expand, ...other } = props;
  return <IconButton {...other} />;
})(({ theme, expand }) => ({
  transform: !expand ? 'rotate(0deg)' : 'rotate(180deg)',
  marginLeft: 'auto',
  transition: theme.transitions.create('transform', {
    duration: theme.transitions.duration.shortest,
  }),
}));

// Vary this variable to make there be less per row or more
const numPerRow = 10

export default function QuantitySelector ({section, index, sectionDetails, setSectionDetails}) {
  const [expanded, setExpanded] = React.useState(false)

  // Contain all seats
  const [seats, setSeats] = React.useState([])

  const [rowsList, setRowsList] = React.useState([])

  const [disableSeats, setDisableSeats] = React.useState(false)

  const handleExpandClick = () => {
    setExpanded(!expanded);
  };

  const handleQuantityChange = (key, qty) => {
    const new_sections = sectionDetails.map((value, key_m) => {
      if (key_m === key) {
        return {...value, quantity: qty}
      }
      return (value)
    })
    setSectionDetails(new_sections)
  }
  

  return (
    <CentredBox sx={{pl: 7, pr: 7, flexDirection: 'column'}}>
      <Grid container  spacing={2} >
        <Grid item xs={10}>
          <Typography sx={{fontSize: 30, fontWeight: 'bold'}}>
            {section.section} - ${section.ticket_price}
          </Typography>
        </Grid>
        <Grid item xs={2}>
          <FormControl fullWidth>
            <Select
              value={section.quantity}
              onChange={(e) => {handleQuantityChange(index, e.target.value)}}
            >
              {section.seats.map((value, key) => {
                return (
                  <MenuItem key={key} value={value}>
                    {value}
                  </MenuItem>
                )
              })}
            </Select>
          </FormControl>
        </Grid>
      </Grid>
      <br/>
    </CentredBox>
  )
}