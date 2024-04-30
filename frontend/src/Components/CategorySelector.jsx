import { Select, useTheme, alpha, MenuItem, Checkbox, ListItemText } from "@mui/material";
import React from "react"
import { apiFetch } from "../Helpers";
import { ContrastInputNoOutline } from "../Styles/InputStyles";



const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;

const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

function getStyles(name, personName, theme) {
  return {
    fontWeight:
      personName.indexOf(name) === -1
        ? theme.typography.fontWeightRegular
        : theme.typography.fontWeightMedium,
  };
}

export default function CategorySelector({selectCategories, setSelectCategories, editable=false}) {
  const theme = useTheme()
  const [categories, setCategories] = React.useState([])

  const getCategories = async () => {
    try {
      const response = await apiFetch('GET', '/api/events/categories/list', null)
      setCategories(response.categories)
    } catch (e) {
      console.log(e)
    }
  }

  React.useEffect(() => {
    getCategories()
  }, [])

  // Handle Categories change
  const handleCategoriesChange = (e) => {
    const {
      target: { value },
    } = e;
    setSelectCategories(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  }
  return (
    <Select
      multiple
      value={selectCategories}
      onChange={handleCategoriesChange}
      input={
        <ContrastInputNoOutline disabled={!editable} sx={{backgroundColor: alpha('#6A7B8A', 0.3)}} multiline rows={4} label='Catergory'/>
      }
      renderValue={(selected) => selected.join(', ')}
      MenuProps={MenuProps}
      fullWidth
      label='Select Categories'
    >
      {categories.map((category) => (
        <MenuItem
          key={category}
          value={category}
          style={getStyles(category, selectCategories, theme)}
        >
          <Checkbox checked={selectCategories.indexOf(category) > -1} />
          <ListItemText primary={category} />
        </MenuItem>
      ))}
    </Select>
  )


}